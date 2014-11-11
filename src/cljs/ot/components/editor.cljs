(ns ot.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [cljs-hash.goog :as gh]
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

(def rejected-keys [8 37 38 39 40])

(defn handle-keypress [e owner {:keys [comm]}]
  (when (not (util/in? rejected-keys (.-keyCode e)))
    (let [key (util/keyFromCode (.-which e))
          op (gen-insert-op key (om/get-state owner :caret) (om/get-state owner :text))]
      (om/update-state! owner :caret inc)
      (om/update-state! owner :text #(documents/apply-ops % op))
      (put! comm op))))

(defn editor-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:comm (chan)
       :caret 0
       :text ""})
    om/IWillMount
    (will-mount [this]
      (queue/init! app)
      (let [comm (om/get-state owner :comm)]
        (go-loop []
          (let [ops (<! comm)]
            (om/update! app [:local-id] (util/unique-id))
            (queue/send ops)
            (recur)))
        (go-loop []
          (let [id (<! queue/sent-ids)]
            (om/transact! app :owned-ids #(conj % id))
            (recur)))
        (go-loop []
          (let [{:keys [local-id version]} (<! queue/recv-ids)]
            (om/transact! app :owned-ids #(vec (remove #{local-id} %)))
            (om/update! app :parent-id [version])))))
    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (dom/textarea #js {:type "text"
                                  :ref "editor"
                                  :value (:text state)
                                  :onKeyPress #(handle-keypress % owner state)})))))


;; ------------------------------------------------------------------------


(defn update-contents! [operations cursor]
  (let [text (get-in @cursor [:input :text])
        new-text (documents/apply-ops text operations)]
    (om/update! cursor [:input :text] new-text)
    (om/update! cursor [:local-id] (util/unique-id))))

(defn editor-view-old [cursor owner]
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
                               (om/update! cursor [:parent-id] [(:id data)])))

           internal-queue ([operations]
                             (update-contents! operations cursor)
                             (queue/send operations))

           queue/sent-ids ([sent-id]
                             (om/transact! cursor :owned-ids #(conj % sent-id)))
           queue/recv-ids ([{:keys [local-id version]}]
                             (println "Updating parent-id to" version)
                             (om/update! cursor [:parent-id] [version])
                             (om/transact! cursor :owned-ids #(remove #{local-id} %))))
          (recur))))
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/div nil
               (om/build input/editor-input 
                         (:input cursor)
                         {:init-state {:comm comm}})))))
