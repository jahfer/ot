(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.util.sockets :as ws]
            [ot.crossover.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(def confirmation (chan))
(def queue (chan))
(def outbound (chan))

(defn component [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                (.log js/console "editor initialized"))
    om/IWillMount
    (will-mount [_]
                 (ws/init!)
                 (ws/event-chan queue (sel1 :#editor) :keypress)
                 (go (while true
                       (let [key (<! outbound)]
                         (.send ws/socket (pr-str (transforms/op :ins key)))))))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea #js {:ref "editor"
                           :id "editor"
                           :defaultValue (-> app :editor :text)
                           :onKeyPress #(put! queue (.-key %))})))))

(defn init-queue []
  (go
    (put! outbound (<! queue))
    (go (while true
          (.log js/console "Waiting...")
          (let [_ (<! confirmation)]
            (.log js/console "Received confirmation, sending outbound!"))))))

(init-queue)
