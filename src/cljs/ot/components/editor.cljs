(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor-input :as input]
            [ot.lib.queue :as queue]
            [ot.documents :as documents]
            [ot.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)]
    (om/transact! cursor [:input :text] (fn [_] new-text))
    (om/transact! cursor :id #(js/md5 new-text))))

(defn editor [cursor owner]
  (reify
    om/IInitState
    (init-state [this]
                {:input (chan)})
    om/IWillMount
    (will-mount [this]
                (queue/init! cursor)
                (let [input (om/get-state owner :input)]
                  (go (loop []
                        (let [operations (<! input)]
                          (update-contents! operations cursor)
                          (queue/send operations)
                          (recur)))))
                (go (loop []
                      (let [operations (<! queue/inbound)
                            buffer (deref queue/buffer)]
                        (update-contents! operations cursor)
                        (swap! queue/buffer transforms/transform operations)
                        (recur))))
                (go (loop []
                      (let [sent-id (<! queue/sent-ids)]
                        (println "Sent ID" sent-id)
                        (om/transact! cursor :owned-ids (fn [coll] (conj coll sent-id)))
                        (recur))))
                (go (loop []
                         (let [recv-id (<! queue/recv-ids)]
                           (om/transact! cursor :owned-ids (fn [coll] (remove #{recv-id} coll)))))))
    om/IRenderState
    (render-state [this {:keys [input]}]
                  (dom/div nil
                           (om/build input/editor-input (:input cursor) {:init-state {:input input}})))))
