(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.crossover.transforms :as transforms]
            ;[ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn component [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                (.log js/console "Initializing editor"))
    om/IWillMount
    (will-mount [_])
                ;(ws/event-chan queue/buffer (sel1 :#editor) :keypress))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea #js {:ref "editor"
                           :id "editor"
                           :defaultValue (-> app :editor :text)
                           :onKeyPress #(put! queue/buffer (.-key %))})))))

(queue/init!)
