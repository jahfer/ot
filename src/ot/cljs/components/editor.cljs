(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.components.editor.input :as input]
            [ot.crossover.documents :as documents]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)]
    (om/transact! cursor [:input :text] (fn [_] new-text))
    (om/transact! cursor :id #(js/md5 new-text))
    (om/transact! cursor :owned-ids (fn [coll] (conj coll (:id @cursor))))))

(defn editor [cursor owner]
  (reify
    om/IInitState
    (init-state [this]
                {:input (chan)})
    om/IWillMount
    (will-mount [this]
                (let [input (om/get-state owner :input)]
                  (go (loop []
                        (let [operations (<! input)]
                          (update-contents! operations cursor)
                          (recur))))))
    om/IRenderState
    (render-state [this {:keys [input]}]
                  (.log js/console "rendering editor" (pr-str cursor))
                  (dom/div nil
                   (om/build input/editor-input (:input cursor) {:init-state {:input input}})))))
