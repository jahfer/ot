(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [cljs-hash.goog :as gh]
            [ot.components.editor-input :as input]
            [ot.lib.queue :as queue]
            [ot.documents :as documents]
            [ot.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)]
    (om/transact! cursor [:input :text] (fn [_] new-text))
    (om/transact! cursor :id #(gh/hash :md5 new-text))))

(defn apply-transform [comm cursor f]
  (go-loop []
    (let [operations (<! comm)]
      (update-contents! operations cursor)
      (f operations)
      (recur))))

(defn apply-internal-transform [comm cursor]
  (apply-transform comm cursor (fn [operations]
                                 (queue/send operations))))

(defn apply-external-transform [comm cursor]
  (apply-transform comm cursor (fn [operations]
                                 (swap! queue/buffer transforms/transform operations))))

(defn add-owned-id [cursor]
  (go-loop []
    (let [sent-id (<! queue/sent-ids)]
      (println "Sent ID" sent-id)
      (om/transact! cursor :owned-ids #(conj % sent-id))
      (recur))))

(defn remove-owned-id [cursor]
  (go-loop []
    (let [recv-id (<! queue/recv-ids)]
      (om/transact! cursor :owned-ids #(remove #{recv-id} %))
      (recur))))

(defn editor [cursor owner]
  (reify
    om/IInitState
    (init-state [this]
                {:comm (chan)})
    om/IWillMount
    (will-mount [this]
      (queue/init! cursor)
      (apply-external-transform queue/inbound cursor)
      (apply-internal-transform (om/get-state owner :comm) cursor)
      (add-owned-id cursor)
      (remove-owned-id cursor))
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/div nil
               (om/build input/editor-input 
                         (:input cursor)
                         {:init-state {:comm comm}})))))
