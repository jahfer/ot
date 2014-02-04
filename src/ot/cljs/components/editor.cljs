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

(defn cursor-position
  "gets or sets the current cursor position"
  ([]
  (.-selectionStart (sel1 :#editor)))
  ([new-pos]
   (let [editor (sel1 :#editor)]
     (set! (.-selectionStart editor) new-pos)
     (set! (.-selectionEnd editor) new-pos))))

(defn doc-length []
  (.. (sel1 :#editor) -value -length))

(defn gen-insert-op [key]
  (let [cursor (js/parseInt (cursor-position) 10)
        retain-before (transforms/op :ret cursor)
        insert (transforms/op :ins key)
        ; Kind of a hack because we don't know the new document length
        ; Only works if we :ins by a single char at a time
        chars-remaining (- (doc-length) cursor)
        retain-after (transforms/op :ret chars-remaining)]
    [retain-before insert retain-after]))

(defn send-insert
  "Pushes the operation to the outbound buffer and updates
  the owned-ids with the new operation ID and the doc-id with
  a hash of the new document contents."
  [owner {:keys [operations id]}]
  (let [parent-id (om/get-state owner :doc-id)
        data (pr-str {:id id :ops operations :parent-id parent-id})
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
        operations (gen-insert-op key)
        new-text (doc/apply-ops operations text)
        cached-cursor (cursor-position)
        id (js/md5 new-text)]
    (send-insert owner {:operations operations :id id})
    (om/set-state! owner :text new-text)
    (cursor-position cached-cursor)))

(defn handle-incoming
  "Processes the inbound queue, applying the operations to
  the current document state."
  [owner]
  (go (while true
        (let [raw (<! queue/inbound)
              data (cljs.reader.read-string (.-data raw))
              ops (:ops data)
              text (om/get-state owner :text)
              cached-cursor (cursor-position)
              new-text (doc/apply-ops ops text)]
          (om/set-state! owner :text new-text)
          (.log js/console "Setting cursor to" cached-cursor)
          (cursor-position cached-cursor)))))


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
