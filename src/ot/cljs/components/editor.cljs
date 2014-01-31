(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.crossover.transforms :as transforms]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn handle-keypress [e owner {:keys [text]}]
  (let [key (util/keyFromCode (.-which e))
        new-text (str text key)
        id (js/md5 new-text)]
    (send-insert owner {:key key :id id})
    (om/set-state! owner :text new-text)
    (om/set-state! owner :doc-id id)))

(defn send-insert [owner {:keys [key id]}]
  (let [op (transforms/op :ins key)
        parent-id (om/get-state owner :doc-id)
        data (pr-str {:id id :op op :parent-id parent-id})
        owned-ids (om/get-state owner :owned-ids)]
    (go (put! queue/buffer data))
    (om/set-state! owner :owned-ids (conj owned-ids id))))

(defn editor-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
                {:text "go"
                 :owned-ids []
                 :doc-id (js/md5 "go")})
    om/IDidMount
    (did-mount [this node]
               (queue/init! owner))
    om/IRenderState
    (render-state [this state]
                  (dom/textarea #js {:id "editor"
                                     :value (:text state)
                                     :onKeyPress #(handle-keypress % owner state)}))))
