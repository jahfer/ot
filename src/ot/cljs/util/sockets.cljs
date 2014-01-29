(ns ot.cljs.util.sockets
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :as reader]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(def send (chan))
(def recv (chan))

(def ws-url "ws://localhost:3000/ws")
(def socket (new js/WebSocket ws-url))

(defn event-chan [c el type]
  (let [writer #(put! c %)]
    (dommy/listen! el type writer)
    {:chan c
     :unsubscribe #(dommy/unlisten! el type writer)}))

(defn make-receiver []
  (set! (.-onmessage socket) (fn [msg] (put! recv msg))))

;; (defn add-message []
;;   (go
;;    (while true
;;      (let [msg (<! recv)
;;            raw-data (.-data msg)
;;            data (reader/read-string raw-data)]
;;        (.log js/console (str data))))))

(defn init! [] (make-receiver))
