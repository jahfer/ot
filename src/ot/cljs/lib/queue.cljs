(ns ot.cljs.lib.queue
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [om.core :as om :include-macros true]
            [ot.crossover.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def confirmation (chan))
(def inbound (chan))
(def buffer-has-item (chan))
(def buffer (atom []))

(defn send [op]
  (.log js/console "Sending operation to buffer for composition")
  (.log js/console "--" (pr-str @buffer))
  (.log js/console "++" (pr-str op))
  (if (empty? @buffer)
    (reset! buffer op)
    (swap! buffer transforms/compose op))
  (.log js/console "==" (pr-str @buffer))
  (.log js/console "............................................................")
  (put! buffer-has-item true))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  []
  (go (while true
    (let [_ (<! confirmation)
          _ (<! buffer-has-item)
          data {:id id :parent-id parent-id :ops @buffer}
          serialized (pr-str data)]
      (.log js/console "[bq <~] Sending operation to the server")
      (ws/send serialized)
      (reset! buffer [])))))

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
  (buffer-queue))
