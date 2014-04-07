(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.components.editor.input :as input]
            [ot.crossover.documents :as documents]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn update-contents! [operations owner]
  (let [text (om/get-state owner :text)
        new-text (documents/apply-ops text operations)]
    (.log js/console (documents/apply-ops text operations))
    (om/set-state! owner :text new-text)
    (om/set-state! owner :id (js/md5 new-text))))

(defn editor [app owner]
  (reify
    om/IInitState
    (init-state [this]
                {:text "Hello"
                 :input (chan)
                 :id nil
                 :owned-ids []})
    om/IWillMount
    (will-mount [this]
                (let [input (om/get-state owner :input)]
                  (go (loop []
                        (let [operations (<! input)]
                          (update-contents! operations owner)
                          (recur))))))
    om/IRenderState
    (render-state [this {:keys [input text]}]
                  (dom/div nil
                   (om/build input/editor-input app {:opts {:on-keypress handle-keypress
                                                            :text text}
                                                     :init-state {:input input}})))))
