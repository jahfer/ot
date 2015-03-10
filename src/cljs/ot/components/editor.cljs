(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.lib.queue2 :as q2]
            [ot.lib.util :as util]
            [ot.routes :as routes]
            [ot.operations :as operations]
            [ot.documents :as documents]
            [ot.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]
                   [jayq.macros :refer [let-ajax]]))

(enable-console-print!)

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

(defn apply-operation! [owner queue id ops]
  (let [last-op (deref (:last-client-op queue))]
    (if-not (seq last-op)
      ;; client hasn't performed actions, can apply cleanly
      (update-text! owner ops)
      ;; client in a buffer state
      ;; need to convert incoming to match what server produces
      (let [[a'' c''] (transforms/transform last-op ops)
            buf (:ops (deref (:buffer queue)))]
        (if (seq buf)
          ;; rebase client queue on server op
          (let [[buf' ops'] (transforms/transform buf c'')]
            (swap! (:buffer queue) assoc :ops buf')
            (update-text! owner ops'))
          ;; nothing to rebase
          (update-text! owner c''))
        ;; keep up to date with transformations
        (reset! (:last-client-op queue) a'')))))

(defn write-parent-id! [owner id]
  (om/set-state! owner :parent-id id))

(defn reset-local-id! [owner]
  (om/set-state! owner :local-id (util/unique-id)))

;; ---------------------------------------------------------------------

(defn editor-com [app owner]
  (reify
    om/IDisplayName (display-name [_] "Editor")
    om/IInitState
    (init-state [this]
      {:comm (chan)
       :local-id (util/unique-id)
       :caret 0
       :parent-id nil})
    om/IWillMount
    (will-mount [this]
      (let [documentid (get-in app [:navigation-data :documentid])
            comm  (om/get-state owner :comm)
            queue (get-in app [:comms :queue])]
        (let-ajax [remote-doc {:url (str (routes/document-path {:id documentid}) ".json")}]
                  (om/set-state! owner :text (:text remote-doc))
                  (om/set-state! owner :parent-id (:version remote-doc)))
        (go-loop []
          ;; outgoing messages
          (let [ops (<! comm)]
            (reset-local-id! owner)
            (q2/send queue {:local-id  (om/get-state owner :local-id)
                            :parent-id (om/get-state owner :parent-id)
                            :ops ops})
            (recur)))
        (go-loop []
          ;; incoming messages
          (let [{:keys [id ops]} (<! (:inbound queue))]
            (apply-operation! owner queue id ops)
            (write-parent-id! owner id)
            (recur)))
        (go-loop []
          ;; roundrip ack
          (let [{:keys [server-id]} (<! (:recv-ids queue))]
            (write-parent-id! owner server-id)
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


