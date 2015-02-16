(ns ot.lib.sockets
  (:require [cljs.core.async :refer [put! chan]]))

(enable-console-print!)

(defn send [ws data]
  (.send (:socket ws) data))

(defn make-receiver [ws]
  (set! (.-onmessage (:socket ws)) (fn [msg]
                                     (when-let [data (.-data msg)]
                                       (put! (:received ws) data)))))

(defn init! [ws] (make-receiver ws))

(defn new-socket [url]
  {:socket (new js/WebSocket url)
   :received (chan)})
