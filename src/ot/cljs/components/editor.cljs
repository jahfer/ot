(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.crossover.transforms :as transforms]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn component [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                (.log js/console "Initializing editor"))
    om/IDidMount
    (did-mount [_ node]
               ;; TODO: Handle storage of ws/event-chan for unsubscription
               (ws/event-chan queue/buffer node :keypress #(str (.-keyCode %))))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea #js {:ref "editor"
                           :id "editor"
                           :defaultValue (-> app :editor :text)})))))

(queue/init!)
