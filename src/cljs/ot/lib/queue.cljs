(ns ot.lib.queue
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.lib.sockets :as ws]
            [ot.lib.util :as util]
            [ot.transit-handlers :as transit-handlers]
            [om.core :as om :include-macros true]
            [ot.composers :as composers]
            [cognitect.transit :as transit])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def confirmation (chan))
(def inbound (chan))
(def buffer-has-item (chan))
(def buffer (atom {}))
(def sent-ids (chan))
(def recv-ids (chan))
(def last-client-op (atom [])) ; last sent operation

(enable-console-print!)

(defn send [data]
  (if (empty? @buffer)
    (do
      (reset! buffer data)
      (put! buffer-has-item true))
    (swap! buffer #(update-in % [:ops] composers/compose (:ops data)))))

(defn flush-buffer! []
  (let [buf-contents @buffer]
    (reset! last-client-op (:ops @buffer))
    (reset! buffer {})
    buf-contents))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  []
  (go-loop []
    (let [_ (<! confirmation)
          _ (<! buffer-has-item)
          buf (flush-buffer!)] ; {:local-id :parent-id :ops}
      (if (seq (:ops buf))
        (let [writer (transit/writer :json {:handlers transit-handlers/write-handlers})
              serialized (transit/write writer buf)]
          (println "[bq <~] Sending operations to the server")
          (put! sent-ids (:local-id buf))
          (ws/send serialized)))
      (recur))))

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
              local-id (:local-id data)
              server-id (:id data)]
          (println "[iq ->] Received operation from server")
          (if (util/in? @owned-ids local-id)
            (do
              (println "  [...] Confirmed operation roundtrip success")
              (put! recv-ids {:local-id local-id :server-id server-id})
              (when (seq (:ops @buffer))
                (swap! buffer assoc :parent-id server-id))
              (put! confirmation response)
              (reset! last-client-op []))
            (put! inbound data))))))

(defn init!
  "Initializes the event queues that manage the in-flow
  and out-flow of events to the editor."
  [cursor]
  (ws/init!)
  (put! confirmation true)
  (recv-queue (:owned-ids cursor))
  (buffer-queue))
