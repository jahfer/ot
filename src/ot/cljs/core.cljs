(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [jayq.util :as jq-util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.cljs.util.sockets :as ws]
            [ot.cljs.components.editor :as editor])
  (:use-macros [dommy.macros :only [node sel sel1]]))

;; Define intial state of app
(def app-state (atom {:editor {:text "Hello world"}}))

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
              (om/build editor/component app {:opts {:comm comm}})))))

(om/root app-state ot-app (sel1 :#app))

(defn listen [el type]
  (let [out (chan)]
    (on ($ el) type
        (fn [e] (put! out e)))
    out))
