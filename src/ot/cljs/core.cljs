(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [dommy.utils :as utils]
            [cljs.reader :as reader]
            [dommy.core :as dommy]
            [jayq.util :as jq-util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.crossover.transforms :as transforms])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:use [jayq.core :only [$ on clone]]))

(def send (chan))
(def receive (chan))

(def ws-url "ws://localhost:3000/ws")
(def ws (new js/WebSocket ws-url))

(defn event-chan [c el type]
  (let [writer #(put! c %)]
    (dommy/listen! el type writer)
    {:chan c
     :unsubscribe #(dommy/unlisten! el type writer)}))

(defn make-sender []
  (event-chan send (sel1 :#websocket) :click)
  (go
    (while true
      (let [evt (<! send)]
        (when (= (.-type evt) "click")
          (.send ws "Test"))))))

(defn make-receiver []
  (set! (.-onmessage ws) (fn [msg] (put! receive msg)))
  (add-message))

(defn add-message []
  (go
   (while true
     (let [msg (<! receive)
           raw-data (.-data msg)
           data (reader/read-string raw-data)]
       (jq-util/log (str data))))))

(defn init! []
  (make-sender)
  (make-receiver))

(defn editor [data owner opts]
  (reify
    om/IInitState
    (init-state [_])
    om/IWillMount
    (will-mount [_]
                (let [{:keys [comm]} opts]
                  (init!)
                  (go (while true
                        (let [key (<! comm)]
                          (jq-util/log (pr-str (transforms/op :ins key))))))))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea
         #js {:ref "editor"
              :id "editor"
              :onKeyPress #(put! comm (.-key %))} "go")))))

(defn ot-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
                (let [comm (chan)]
                  (om/set-state! owner :comm comm)))
    om/IWillUpdate
    (will-update [_ _ _])
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRender
    (render [_]
            (let [comm (om/get-state owner :comm)]
              (om/build editor app {:opts {:comm comm}})))))

(def app-state (atom {:text "Hello world"}))

(om/root app-state ot-app (sel1 :#app))

(defn listen [el type]
  (let [out (chan)]
    (on ($ el) type
        (fn [e] (put! out e)))
    out))
