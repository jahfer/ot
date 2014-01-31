(ns ot.cljs.lib.operation-queue
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def confirmation (chan))
(def buffer (chan))
(def outbound (chan))
(def inbound (chan))

(defn outbound-queue
  "Routine for sending commands to the server. Converts
  to an operation, and transforms to the literal string
  version before sending."
  []
  (go (while true
        (let [data (<! outbound)]
          (.log js/console "[oq <-] Sending operation to the server")
          (ws/send data)))))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  []
  (go (while true
        (let [_ (<! confirmation)]
          (let [out (<! buffer)]
            (.log js/console "[bq <~] Sending operation to outbound")
            (put! outbound out))))))

(defn recv-queue
  "Routine to receive operations from server. When the
  ID is owned by the client, it is removed from the
  owned-ids queue, and added to the queue of confirmed
  operations."
  [owner]
  (go (while true
        (let [response (<! ws/recv)
              data (cljs.reader/read-string (.-data response))
              id (:id data)
              owned-ids (om/get-state owner :owned-ids)]
          (.log js/console "[iq ->] Received operation from server")
          (if (util/in? owned-ids id)
            (do
              (.log js/console "  [...] Confirmed operation roundtrip success")
              (om/set-state! owner :owned-ids (remove #{id} owned-ids))
              (put! confirmation response))
            (put! inbound response))))))
(defn init!
  "Initializes the event queues that manage the in-flow
  and out-flow of events to the editor."
  [owner]
  (ws/init!)
  (put! confirmation true)
  (recv-queue owner)
  (buffer-queue)
  (outbound-queue))
