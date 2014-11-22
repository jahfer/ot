(ns ot.lib.sockets
  (:require [cljs.core.async :refer [put! chan]]))

(def recv (chan))
(def ws-url "ws://localhost:3000/editor/ws")
(def socket (new js/WebSocket ws-url))

(defn make-receiver []
  (set! (.-onmessage socket) (fn [msg]
                               (when-let [data (.-data msg)]
                                 (put! recv data)))))

(defn send [data]
  (.send socket data))

(defn init! [] (make-receiver))
