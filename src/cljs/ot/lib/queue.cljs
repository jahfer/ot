(ns ot.lib.queue
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.lib.sockets :as ws]
            [ot.lib.util :as util]
            [ot.transit-handlers :as transit-handlers]
            [om.core :as om :include-macros true]
            [ot.transforms :as transforms]
            [cognitect.transit :as transit])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def confirmation (chan))
(def inbound (chan))
(def buffer-has-item (chan))
(def buffer (atom []))
(def sent-ids (chan))
(def recv-ids (chan))
(def last-client-op (atom []))

(enable-console-print!)

(defn send [op]
  (if (empty? @buffer)
    (do
      (reset! buffer op)
      (put! buffer-has-item true))
    (swap! buffer transforms/compose op)))

(defn flush-buffer! []
  (let [buf-contents @buffer]
    (reset! last-client-op @buffer)
    (reset! buffer [])
    buf-contents))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  [[id parent-id]]
  (go (while true
    (let [_ (<! confirmation)
          _ (<! buffer-has-item)
          buf (flush-buffer!)]
      (if (seq buf)
        (let [data {:id @id :parent-id @parent-id :ops buf}
              writer (transit/writer :json {:handlers transit-handlers/write-handlers})
              serialized (transit/write writer data)]
          (println "[bq <~] Sending operation to the server")
          (put! sent-ids @id)
          (ws/send serialized)))))))

(defn recv-queue
  "Routine to receive operations from server. When the
  ID is owned by the client, it is removed from the
  owned-ids queue, and added to the queue of confirmed
  operations."
  [owned-ids]
  (go (while true
        (let [response (<! ws/recv)
              reader (transit/reader :json {:handlers transit-handlers/read-handlers})
              data (transit/read reader response)
              id (:id data)]
          (println "[iq ->] Received operation from server")
          (if (util/in? @owned-ids id)
            (do
              (println "  [...] Confirmed operation roundtrip success")
              (put! recv-ids id)
              (put! confirmation response)
              (reset! last-client-op []))
            (put! inbound (:ops data)))))))

(defn init!
  "Initializes the event queues that manage the in-flow
  and out-flow of events to the editor."
  [cursor]
  (ws/init!)
  (put! confirmation true)
  (recv-queue (:owned-ids cursor))
  (buffer-queue [(:id cursor) (:parent-id cursor)]))
