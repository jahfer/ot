(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [cljs-hash.goog :as gh]
            [ot.components.editor-input :as input]
            [ot.lib.queue :as queue]
            [ot.documents :as documents]
            [ot.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(enable-console-print!)

(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)
        current-id (:id @cursor)]
    (om/transact! cursor [:input :text] (fn [_] new-text))
    (om/transact! cursor :id #(gh/hash :md5 new-text))
    (om/transact! cursor :parent-id (fn [_] current-id))))

(defn editor [cursor owner]
  (reify
    om/IInitState
    (init-state [this]
                {:comm (chan)})
    om/IWillMount
    (will-mount [this]
      (queue/init! cursor)
      (go-loop []
        (let [external-queue queue/inbound
              internal-queue (om/get-state owner :comm)]
          (alt!
           external-queue ([operations]
                             (let [last-op (deref queue/last-client-op)
                                   buf (deref queue/buffer)]
                               (if (seq last-op)
                                 (let [[a'' c''] (transforms/transform last-op operations)]
                                   (if (seq buf)
                                     (let [[buf' ops'] (transforms/transform buf c'')]
                                       (reset! queue/buffer buf')
                                       (update-contents! ops' cursor))
                                     (update-contents! c'' cursor))
                                   (reset! queue/last-client-op a''))
                                 (update-contents! operations cursor))))
           internal-queue ([operations]
                             (update-contents! operations cursor)
                             (queue/send operations))
           queue/sent-ids ([sent-id]
                             (om/transact! cursor :owned-ids #(conj % sent-id)))
           queue/recv-ids ([recv-id]
                             (om/transact! cursor :owned-ids #(remove #{recv-id} %))))
          (recur))))
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/div nil
               (om/build input/editor-input 
                         (:input cursor)
                         {:init-state {:comm comm}})))))
