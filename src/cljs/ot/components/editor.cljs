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

(defn unique-id []
  (loop [id ""]
    (let [new-id (+ id (.substr (.toString (.random js/Math) 36) 2))]
      (if (> (.-length new-id) 8)
        (+ "client-" new-id)
        (recur new-id)))))

(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)]
    (om/update! cursor [:input :text] new-text)
    (om/update! cursor [:local-id] (unique-id))))

(defn editor [cursor owner]
  (reify
    om/IInitState
    (init-state [this]
                {:comm (chan)})
    om/IWillMount
    (will-mount [this]
      (println cursor)
      (queue/init! cursor)
      (go-loop []
        (let [external-queue queue/inbound
              internal-queue (om/get-state owner :comm)]
          (alt!
           external-queue ([data]
                             (let [operations (:ops data)
                                   last-op (deref queue/last-client-op)
                                   buf (deref queue/buffer)]
                               (if (seq last-op)
                                        ; client in a buffer state
                                        ; need to convert incoming to match what server produces 
                                 (let [[a'' c''] (transforms/transform last-op operations)]
                                   (if (seq buf)
                                        ; rebase client queue on server op
                                     (let [[buf' ops'] (transforms/transform buf c'')]
                                       (reset! queue/buffer buf')
                                       (update-contents! ops' cursor)
                                       (println "received: " operations))
                                        ; nothing to rebase
                                     (update-contents! c'' cursor))
                                        ; keep up to date with transformations
                                   (reset! queue/last-client-op a''))
                                        ; client hasn't performed actions, can apply cleanly
                                 (update-contents! operations cursor))
                                        ; acknowledge that we're on the latest parent
                               (println "Updating parent-id to" (:id data))
                               (om/update! cursor :parent-id [(:id data)])))

           internal-queue ([operations]
                             (update-contents! operations cursor)
                             (queue/send operations))

           queue/sent-ids ([sent-id]
                             (om/transact! cursor :owned-ids #(conj % sent-id)))
           queue/recv-ids ([{:keys [local-id version]}]
                             (println "Updating parent-id to" version)
                             (om/update! cursor :parent-id [version])
                             (om/transact! cursor :owned-ids #(remove #{local-id} %))))
          (recur))))
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/div nil
               (om/build input/editor-input 
                         (:input cursor)
                         {:init-state {:comm comm}})))))
