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

(def rejected-keys ["Up" "Down" "Left" "Right"])

(def cursor (atom 0))

(defn cursor-position
  "gets or sets the current cursor position"
  ([]
  (.-selectionStart (sel1 :#editor)))
  ([new-pos]
   (let [editor (sel1 :#editor)]
     (.setSelectionRange editor new-pos new-pos))))

(defn doc-length []
  (.. (sel1 :#editor) -value -length))

(defn gen-insert-op [key]
  (let [cursor-loc (util/toInt (cursor-position))
        retain-before (transforms/op :ret cursor-loc)
        insert (transforms/op :ins key)
        ; Kind of a hack because we don't know the new document length
        ; Only works if we :ins by a single char at a time
        chars-remaining (- (doc-length) cursor-loc)
        retain-after (transforms/op :ret chars-remaining)]
    [retain-before insert retain-after]))

(defn send-insert
  "Pushes the operation to the outbound buffer and updates
  the owned-ids with the new operation ID and the doc-id with
  a hash of the new document contents."
  [owner {:keys [operations id]}]
  (go (queue/send operations))
  (om/set-state! owner :owned-ids (conj owned-ids id))
  (om/set-state! owner :doc-id id))

(defn handle-keypress
  "Manages user input into the editor. Determines the key
  pressed, and the new document id. Sets the textarea value
  to the new result and dispatches an insert to the server."
  [e owner {:keys [text]}]
  (when (not (util/in? rejected-keys (.-key e)))
    (let [key (util/keyFromCode (.-which e))
          operations (gen-insert-op key)
          new-text (doc/apply-ops text operations)
          id (js/md5 new-text)]
      (send-insert owner {:operations operations :id id})
      (swap! cursor #(-> (cursor-position) util/toInt inc))
      (om/set-state! owner :text new-text))))

(defn handle-incoming
  "Processes the inbound queue, applying the operations to
  the current document state."
  [owner]
  (go (while true
        (let [raw (<! queue/inbound)
              _ (.log js/console "Processing server input as new operation")
              data (cljs.reader.read-string (.-data raw))
              ops (:ops data)
              text (om/get-state owner :text)
              cached-cursor (cursor-position)
              new-text (doc/apply-ops text ops)]
          (om/set-state! owner :text new-text)))))

(defn editor-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
                {:text ""
                 :owned-ids []
                 :doc-id (js/md5 "")})
    om/IWillMount
    (will-mount [this]
                (handle-incoming owner))
    om/IDidMount
    (did-mount [this]
               (queue/init! owner))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
                (cursor-position @cursor))
    om/IRenderState
    (render-state [this state]
                  (dom/textarea #js {:id "editor"
                                     :value (:text state)
                                     :onKeyPress #(handle-keypress % owner state)}))))
