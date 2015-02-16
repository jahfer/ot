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
   :recv-ids        (chan)
   :owned-ids       (atom [])
   :buffer          (atom {})
   :last-client-op  (atom [])})

(defn- poll-incoming [queue]
  (go-loop []
    (let [response (<! (get-in queue [:ws :received]))
          reader (transit/reader :json {:handlers transit-handlers/read-handlers})
          data (transit/read reader response)
          local-id (:local-id data)
          server-id (:id data)
          owned-ids (:owned-ids queue)]
      (println "[iq ->] Received operation from server" local-id)
      (if (util/in? @owned-ids local-id)
        (do
          (println "  [...] Confirmed operation roundtrip success")
          (when (seq (:ops (deref (:buffer queue))))
            (swap! (:buffer queue) assoc :parent-id server-id))
          (swap! (:owned-ids queue) #(vec (remove #{local-id} %)))
          (reset! (:last-client-op queue) [])
          (put! (:confirmation queue) response)
          (put! (:recv-ids queue) {:server-id server-id}))
        (put! (:inbound queue) data)))
    (recur)))

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
      (let [out (flush-buffer! buffer)]
        (set-last-op! (:last-client-op queue) (:ops out))
        (when (seq (:ops out))
          (let [writer (transit/writer :json {:handlers transit-handlers/write-handlers})
                serialized (transit/write writer out)]
            (println "[bq <~] Sending operations to the server" (:local-id out))
            (swap! (:owned-ids queue) #(conj % (:local-id out)))
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

(defn init! [queue]
  (ws/init! (:ws queue))
  (poll-incoming queue)
  queue)

(defn new-queue [ws-url]
  (let [state (-> (build-state)
                  (assoc :ws (ws/new-socket ws-url)))]
    (let [ackc (throttle-outgoing state)
          queue (assoc state :confirmation ackc)]
      (put! ackc true)
      (init! queue))))
