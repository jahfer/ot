(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor-input :as input]
            [ot.lib.queue :as queue]
            [ot.lib.util :as util]
            [ot.operations :as operations]
            [ot.documents :as documents]
            [ot.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(enable-console-print!)

(defn editor [editor owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [text] :as state}]
      (dom/textarea #js {:id "editor"
                         :value text}))))

;; ---------------------------------------------------------------------

(defn caret-position
  "gets or sets the current cursor position"
  ([owner]
   (util/toInt (.-selectionStart (om/get-node owner "editor"))))
  ([owner new-pos]
   (let [editor (om/get-node owner "editor")]
     (.setSelectionRange editor new-pos new-pos))))

(defn gen-insert-op [key caret text]
  (let [op-list (operations/oplist :ret caret :ins key)
        chars-remaining (- (count text) caret)]
    (if (zero? chars-remaining)
      op-list
      (conj op-list (operations/->Op :ret chars-remaining)))))

(defn gen-delete-op [caret text]
  (let [op-list (operations/oplist :ret (dec caret) :del 1)
        chars-remaining (- (count text) caret)]
    (if (zero? chars-remaining)
      op-list
      (conj op-list (operations/->Op :ret chars-remaining)))))

;; ---------------------------------------------------------------------

(def rejected-keys [8 37 38 39 40])

(defn update-text! [owner ops]
  (om/update-state! owner :text #(documents/apply-ops % ops)))

(defn handle-keypress [e owner {:keys [comm]}]
  (when (not (util/in? rejected-keys (.-keyCode e)))
    (om/set-state! owner :caret (caret-position owner))
    (let [key (util/keyFromCode (.-which e))
          op (gen-insert-op key (om/get-state owner :caret) (om/get-state owner :text))]
      (om/update-state! owner [:caret] inc)
      (update-text! owner op)
      (put! comm op))))

(defn handle-keydown [e owner {:keys [comm]}]
  (om/set-state! owner :caret (caret-position owner))
  (when (= 8 (.-which e))
    (let [op (gen-delete-op (om/get-state owner :caret) (om/get-state owner :text))]
      (om/update-state! owner [:caret] dec)
      (update-text! owner op)
      (put! comm op))))

(defn editor-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:comm (chan)
       :local-id (util/unique-id)
       :caret 0})
    om/IWillMount
    (will-mount [this]
      (queue/init! app)
      (let [comm (om/get-state owner :comm)]
        (go-loop []
          (let [ops (<! comm)]
            (om/set-state! owner :local-id (util/unique-id))
            (queue/send {:local-id (om/get-state owner :local-id)
                         :parent-id (om/get-state owner :parent-id)
                         :ops ops})
            (recur)))
        (go-loop []
          (let [{:keys [id ops]} (<! queue/inbound)
                last-op (deref queue/last-client-op)]
            (if (seq last-op)
                                        ; client in a buffer state
                                        ; need to convert incoming to match what server produces
              (let [[a'' c''] (transforms/transform last-op ops)
                    buf (:ops (deref queue/buffer))]
                (if (seq buf)
                                        ; rebase client queue on server op
                  (let [[buf' ops'] (transforms/transform buf c'')]
                    (swap! queue/buffer assoc :ops buf')
                    (update-text! owner ops'))
                                        ; nothing to rebase
                  (update-text! owner c''))
                                        ; keep up to date with transformations
                (reset! queue/last-client-op a''))
                                        ; client hasn't performed actions, can apply cleanly
              (update-text! owner ops))
                                        ; acknowledge that we're on the latest parent
            (om/set-state! owner :parent-id id)
            (recur)))
        (go-loop []
          (let [id (<! queue/sent-ids)]
            (om/transact! app :owned-ids #(conj % id))
            (recur)))
        (go-loop []
          (let [{:keys [local-id server-id]} (<! queue/recv-ids)]
            (om/transact! app :owned-ids #(vec (remove #{local-id} %)))
            (om/set-state! owner :parent-id server-id)
            (recur)))))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (caret-position owner (om/get-state owner :caret)))
    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (dom/textarea #js {:type "text"
                                  :id "editor"
                                  :ref "editor"
                                  :value (:text state)
                                  :onClick #(om/set-state! owner :caret (caret-position owner))
                                  :onKeyPress #(handle-keypress % owner state)
                                  :onKeyDown #(handle-keydown % owner state)})))))


