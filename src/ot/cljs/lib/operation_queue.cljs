(ns ot.cljs.lib.operation-queue
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.crossover.transforms :as transforms]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def confirmation (chan))
(def buffer (chan))
(def outbound (chan))
(def owned-ids '())

(defn outbound-queue
  "Routine for sending commands to the server. Converts
  to an operation, and transforms to the literal string
  version before sending."
  []
  (go (while true
        (let [key (<! outbound)
              id (js/md5 key)
              op (transforms/op :ins key)
              data (pr-str {:id id :op op})]
          (.log js/console "[outbound-q] Sending operation to the server")
          (def owned-ids (conj owned-ids id))
          (ws/send data)))))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  []
  (go (while true
        (let [_ (<! confirmation)]
          (let [out (<! buffer)]
            (.log js/console "[buffer-q] Sending operation to outbound")
            (put! outbound out))))))

(defn inbound-queue
  "Routine to receive operations from server. When the
  ID is owned by the client, it is removed from the
  owned-ids queue, and added to the queue of confirmed
  operations."
  []
  (go (while true
        (let [response (<! ws/recv)
              data (cljs.reader/read-string (.-data response))
              id (:id data)]
          (.log js/console "[inbound-queue] Received operation from server")
          (if (util/in? owned-ids id)
            (do
              (.log js/console "[inbound-queue] Confirmed operation roundtrip success")
              (def owned-ids (remove #{id} owned-ids))
              (put! confirmation response))
            true)))))


(defn init! []
  (ws/init!)
  (put! confirmation true)
  (inbound-queue)
  (buffer-queue)
  (outbound-queue))
