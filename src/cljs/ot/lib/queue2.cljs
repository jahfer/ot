(ns ot.lib.queue2
  (:require [cljs.core.async :refer [put! chan <!]]
            [ot.lib.sockets :as ws]
            [ot.lib.util :as util]
            [ot.transit-handlers :as transit-handlers]
            [ot.composers :as composers]
            [om.core :as om :include-macros true]
            [cognitect.transit :as transit])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn- build-state []
  {:inbound         (chan)
   :buffer-has-item (chan)
   :sent-ids        (chan)
   :recv-ids        (chan)
   :buffer          (atom {})
   :last-client-op  (atom [])})

(defn- poll-incoming [queue owned-ids-lens]
  (go-loop []
    (let [response (<! (get-in queue [:ws :received]))
          _ (println "poll-incoming")
          reader (transit/reader :json {:handlers transit-handlers/read-handlers})
          data (transit/read reader response)
          local-id (:local-id data)
          server-id (:id data)]
      (println "[iq ->] Received operation from server")
      (if (util/in? @owned-ids-lens local-id)
        (do
          (println "  [...] Confirmed operation roundtrip success")
          (when (seq (:ops (deref (:buffer queue))))
            (println "poll-incoming updating parent-id")
            (swap! (:buffer queue) assoc :parent-id server-id))
          (reset! (:last-client-op queue) [])
          (put! (:confirmation queue) response)
          (put! (:recv-ids queue) {:local-id local-id :server-id server-id}))
        (put! (:inbound queue) data)))))

(defn- set-last-op! [last-op-atom op]
  (reset! last-op-atom op))

(defn- flush-buffer! [buffer]
  (let [out @buffer]
    (reset! buffer {})
    out))

(defn- throttle-outgoing [queue]
  (let [ackc (chan)
        buffer (:buffer queue)
        buffer-has-item (:buffer-has-item queue)]
    (go-loop []
      (<! ackc)
      (<! buffer-has-item)
      (println "Have operation to send")
      (let [out (flush-buffer! buffer)]
        (set-last-op! (:last-client-op queue) (:ops out))
        (when (seq (:ops out))
          (let [writer (transit/writer :json {:handlers transit-handlers/write-handlers})
                serialized (transit/write writer out)]
            (println "[bq <~] Sending operations to the server")
            (put! (:sent-ids queue) (:local-id out))
            (ws/send (:ws queue) serialized)))
        (recur)))
    ackc))

(defn send [queue data]
  (let [buffer (:buffer queue)]
    (if (empty? @buffer)
      (do
        (reset! buffer data)
        (put! (:buffer-has-item queue) true))
      (swap! buffer #(update-in % [:ops] composers/compose (:ops data))))))

(defn init! [queue cursor]
  (poll-incoming queue (:owned-ids cursor)))

(defn new-queue [ws-url]
  (let [state (-> (build-state)
                  (assoc :ws (ws/new-socket ws-url)))]
    (ws/init! (:ws state))
    (let [ackc (throttle-outgoing state)]
      (put! ackc true)
      (assoc state :confirmation ackc))))
