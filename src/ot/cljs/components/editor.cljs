(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.util.sockets :as ws]
            [ot.crossover.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn component [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                (.log js/console "editor initialized"))
    om/IWillMount
    (will-mount [_]
                 (ws/init!)
                 (let [{:keys [comm]} opts]
                  (ws/event-chan comm (sel1 :#editor) :keypress)
                  (go (while true
                        (let [key (<! comm)]
                          (.send ws/socket (pr-str (transforms/op :ins key))))))))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea #js {:ref "editor"
                           :id "editor"
                           :defaultValue (-> app :editor :text)
                           :onKeyPress #(put! comm (.-key %))})))))
