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
(def sent-ids (chan))
(def recv-ids (chan))

(enable-console-print!)

(defn send [op]
  (if (empty? @buffer)
    (do
      (reset! buffer op)
      (put! buffer-has-item true))
    (swap! buffer transforms/compose op)))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  [id]
  (go (while true
    (let [_ (<! confirmation)
          _ (<! buffer-has-item)
          data {:id @id :parent-id parent-id :ops @buffer}
          serialized (pr-str data)]
      (println "[bq <~] Sending operation to the server")
      (put! sent-ids @id)
      (ws/send serialized)
      (reset! buffer [])))))

(defn recv-queue
  "Routine to receive operations from server. When the
  ID is owned by the client, it is removed from the
  owned-ids queue, and added to the queue of confirmed
  operations."
  [owned-ids]
  (cljs.reader/register-tag-parser! 'ot.crossover.transforms.Op transforms/map->Op)
  (go (while true
        (let [response (<! ws/recv)
              data (cljs.reader/read-string (.-data response))
              id (:id data)]
          (println "[iq ->] Received operation from server")
          (if (util/in? @owned-ids id)
            (do
              (println "  [...] Confirmed operation roundtrip success")
              (put! recv-ids id)
              (put! confirmation response))
            (do
              (put! inbound (:ops data))))))))

(defn init!
  "Initializes the event queues that manage the in-flow
  and out-flow of events to the editor."
  [cursor]
  (ws/init!)
  (put! confirmation true)
  (recv-queue (:owned-ids cursor))
  (buffer-queue (:id cursor)))
