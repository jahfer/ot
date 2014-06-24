(ns ot.lib.sockets
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cljs.reader :as reader]
            [dommy.utils :as utils]
            [dommy.core :as dommy])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(def recv (chan))

(def ws-url "ws://localhost:3000/editor/ws")
(def socket (new js/WebSocket ws-url))

(defn event-chan
  ([c el type]
   (event-chan c el type #(%)))
  ([c el type transform]
   (let [writer #(put! c (transform %))]
     (dommy/listen! el type writer)
     {:chan c
      :unsubscribe #(dommy/unlisten! el type writer)})))

(defn make-receiver []
  (set! (.-onmessage socket) (fn [msg] (put! recv msg))))

(defn send [data]
  (.send socket data))

(defn init! [] (make-receiver))
