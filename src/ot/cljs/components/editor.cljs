(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.crossover.transforms :as transforms]
            [ot.crossover.documents :as doc]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.lib.util :as util]
            [ot.cljs.lib.operation-queue :as queue])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn send-insert
  "Pushes the operation to the outbound buffer and updates
  the owned-ids with the new operation ID and the doc-id with
  a hash of the new document contents."
  [owner {:keys [key id]}]
  (let [ret-count (.-selectionStart (sel1 :#editor))
        ops [(transforms/op :ret ret-count) (transforms/op :ins key)]
        parent-id (om/get-state owner :doc-id)
        data (pr-str {:id id :ops ops :parent-id parent-id})
        owned-ids (om/get-state owner :owned-ids)]
    (go (put! queue/buffer data))
    (om/set-state! owner :owned-ids (conj owned-ids id))
    (om/set-state! owner :doc-id id)))

; TODO: refactor so handle-keypress and handle-incoming both
;       use doc/apply-ops. handle-keypress should generate
;       the operation string to push to send-insert.
(defn handle-keypress
  "Manages user input into the editor. Determines the key
  pressed, and the new document id. Sets the textarea value
  to the new result and dispatches an insert to the server."
  [e owner {:keys [text]}]
  (let [key (util/keyFromCode (.-which e))
        new-text (str text key)
        id (js/md5 new-text)]
    (send-insert owner {:key key :id id})
    (om/set-state! owner :text new-text)))

(defn handle-incoming
  "Processes the inbound queue, applying the operations to
  the current document state."
  [owner]
  (go (while true
        (let [raw (<! queue/inbound)
              data (cljs.reader.read-string (.-data raw))
              ops (:ops data)
              text (om/get-state owner :text)
              applied (doc/apply-ops ops text)]
          (om/set-state! owner :text applied)))))

(defn editor-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
                {:text "go"
                 :owned-ids []
                 :doc-id (js/md5 "go")})
    om/IWillMount
    (will-mount [this]
                (handle-incoming owner))
    om/IDidMount
    (did-mount [this node]
               (queue/init! owner))
    om/IRenderState
    (render-state [this state]
                  (dom/textarea #js {:id "editor"
                                     :value (:text state)
                                     :onKeyPress #(handle-keypress % owner state)}))))
