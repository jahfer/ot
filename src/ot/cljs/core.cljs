(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [jayq.util :as jq-util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:use [jayq.core :only [$ on clone]]))

(defn widget [data owner opts]
  (let [{:keys [comm]} opts]
    (om/component
     (dom/textarea
      #js {:ref "editor"
           :id "editor"
           :onKeyPress #(put! comm (.-key %))}))))

(defn ot-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
                (let [comm (chan)]
                  (jq-util/log app)
                  (om/set-state! owner :comm comm)
                  (go (while true
                        (let [key (<! comm)]
                          (jq-util/log key))))))
    om/IWillUpdate
    (will-update [_ _ _])
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRender
    (render [_]
            (let [comm (om/get-state owner :comm)]
              (om/build widget app {:opts {:comm comm}})))))

(def app-state (atom {:text "Hello world"}))

(om/root app-state ot-app (sel1 :#app))

;; (defn listen [el type]
;;   (let [out (chan)]
;;     (on ($ el) type
;;         (fn [e] (put! out e)))
;;     out))

;; (defn post [uri params]
;;   (let [out (chan)]
;;     (ajax uri
;;           {:type "POST"
;;            :data params
;;            :contentType "application/edn"
;;            :success (fn [res] (put! out res))})
;;     out))

;; (defn init []
;;   (let [keypress (listen (om/get-node "editor") "keypress")]
;;     (go (while true
;;           (let [e (<! keypress)
;;                 key (.-key e)]
;;             (jq-util/log (.-key e))
;;             (let [results (<! (post "/live" {:type :ins :val key}))]
;;               (jq-util/log (pr-str results))))))))

;; (init)