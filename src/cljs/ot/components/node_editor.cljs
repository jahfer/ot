(ns ot.components.node-editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.pprint :refer [pprint]]
            [ot.routes :as routes]
            [ot.lib.util :as util]
            [ot.lib.queue :as q]
            [cljs.core.async :refer [chan <! put!]]
            [othello.documents :as documents]
            [othello.operations :as operations]
            [othello.transforms :as transforms])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [jayq.macros :refer [let-ajax]]))

(enable-console-print!)

(def current-user :123)
(def frame-id (rand-int 100))

(defn caret-position
  "gets or sets the current cursor position"
  ([]
   (util/toInt (.-startOffset (.getRangeAt (.getSelection js/window) 0))))
  ([node new-pos]
   (let [el (aget (.-childNodes node) 0)
         range (.createRange js/document)
         sel (.getSelection js/document)]
     (.setStart range el new-pos)
     (.collapse range true)
     (.removeAllRanges sel)
     (.addRange sel range))))

(defn handle-keypress [e node owner]
  (when (not (util/in? [8 37 38 39 40] (.-keyCode e)))
    (let [char (util/keyFromCode (.-which e))
          caret (caret-position)
          op-offset (+ caret (:start-offset node))]
      (println frame-id "Pressed" char "@" op-offset)
      (let [ops (operations/oplist
                  ::operations/ret op-offset
                  ::operations/ins char
                  ;; should retain over the length of the document!!
                  ::operations/ret (- (:length @node) caret))
            update-ch (om/get-state owner :update)]
        (put! update-ch {:type :insert :ops ops :node node})
        (om/transact! node :length inc)
        (println frame-id "node length updated" (:length node) (:length @node))
        (om/transact! node [:authors current-user :offset] inc)))
    (.preventDefault e)))

(defn handle-keydown [e node owner]
  (when (util/in? [8 46] (.-which e))
    (let [caret (dec (caret-position))
          op-offset (+ caret (:start-offset node))
          ops (operations/oplist
                ::operations/ret op-offset
                ::operations/del 1
                ;; should retain over the length of the document!!
                ::operations/ret (- (:length @node) caret))
          update-ch (om/get-state owner :update)]
      (println frame-id "Pressed DELETE @" (+ caret (:start-offset node)))
      (put! update-ch {:type :delete :ops ops :node node})
      (om/transact! node :length dec)
      (om/transact! node [:authors current-user :offset] dec))
    (.preventDefault e)))

(defn track-author-cursor! [e authors]
  (om/transact! authors :cursors
                #(assoc % current-user {:id 123
                                        :name "Jahfer"
                                        :node (.-target e)
                                        :offset (caret-position)})))

(defn move-cursor! [author owner]
  (when-not (= (:offset author) (om/get-state owner :last-offset))
    (let [range (.createRange js/document)
          inner-node (aget (.-childNodes (:node author)) 0)]
      (.setStart range inner-node (:offset author))
      (.setEnd range inner-node (inc (:offset author)))
      (let [bounding (.getBoundingClientRect range)]
        (om/set-state! owner :last-offset (:offset author))
        (om/set-state! owner :position {:top (str (.-top bounding) "px")
                                        :left (str (.-left bounding) "px")})))))

(defn author-cursor [author owner]
  (reify
    om/IDisplayName (display-name [_] "CursorNode")
    om/IDidMount
    (did-mount [_] (move-cursor! author owner))
    om/IDidUpdate
    (did-update [_ _ _] (move-cursor! author owner))
    om/IRenderState
    (render-state [_ {:keys [position]}]
            (dom/div #js {:className "author-cursor"
                          :style #js {:top (:top position)
                                      :left (:left position)
                                      :height "18px"}}
                     (dom/span #js {:className "author"} (:name author))))))

(defn text-node [node owner]
  (reify
    om/IDisplayName (display-name [_] "TextNode")
    om/IRender
    (render [_]
      (dom/span nil
                (dom/span #js {:ref "node-content"
                               :contentEditable true
                               :onKeyPress #(handle-keypress % node owner)
                               :onKeyDown #(handle-keydown % node owner)}
                          (:data node))))))

(defn link-node [node _]
  (reify
    om/IDisplayName (display-name [_] "LinkNode")
    om/IRender
    (render [_]
      (let [{:keys [alt href text]} (:data node)]
        (dom/a #js {:alt alt :href href} text)))))

(defn component-for-node [node-type]
  (case node-type
    ::documents/text text-node
    :link link-node))

(defn write-parent-id! [owner id]
  (om/set-state! owner :parent-id id))

(defn update-text! [nodes ops]
  (when (= (-> ops first :type) ::operations/ret)
    (let [retain-length (-> ops first :val)
          active-node (reduce (fn [acc node]
                                (let [sum (+ acc (:length @node))]
                                  (println frame-id "looking for" retain-length "have" sum)
                                  (if (>= sum retain-length) (reduced node) sum)))
                              0 nodes)]
      (println frame-id "Updating text for node" active-node)
      (om/transact! active-node :data #(documents/apply-ops % ops))
      (om/update! active-node :length (count (:data @active-node))))))

(defn apply-operation! [nodes queue ops]
  (let [last-op (deref (:last-client-op queue))]
    (if-not (seq last-op)
      ;; client hasn't performed actions, can apply cleanly
      (update-text! nodes ops)
      ;; client in a buffer state
      ;; need to convert incoming to match what server produces
      (let [[a'' c''] (transforms/transform last-op ops)
            buf (:ops (deref (:buffer queue)))]
        (if (seq buf)
          ;; rebase client queue on server op
          (let [[buf' ops'] (transforms/transform buf c'')]
            (swap! (:buffer queue) assoc :ops buf')
            (update-text! nodes ops'))
          ;; nothing to rebase
          (update-text! nodes c''))
        ;; keep up to date with transformations
        (reset! (:last-client-op queue) a'')))))

(defn node-editor [app owner]
  (reify
    om/IDisplayName (display-name [_] "NodeEditor")
    om/IInitState
    (init-state [_]
                {:update (chan)
                 :local-id (util/unique-id)
                 :parent-id nil})
    om/IWillMount
    (will-mount [_]
                (let [queue (:queue app)
                      documentid (get-in app [:navigation-data :documentid])]
                  (let-ajax [remote-doc {:url (str (routes/document-path {:id documentid}) ".json")}]
                            (om/set-state! owner :parent-id (:deltaid remote-doc)))
                  ;; outgoing messages
                  (go-loop []
                    (let [update-ch (om/get-state owner :update)
                          {:keys [node type ops]} (<! update-ch)
                          relative-ops (update-in ops [0 :val] #(- % (:start-offset node)))
                          current-user (get-in app [:editor :authors :current-user])]
                      ;; apply message to local state
                      (om/transact! node :data #(documents/apply-ops % relative-ops))
                      (om/transact! (:editor app) [:authors :cursors current-user :offset]
                                    (if (= type :delete) dec inc))
                      ;; prepare and send event to server
                      (om/set-state! owner :local-id (util/unique-id))
                      (q/send queue {:local-id (om/get-state owner :local-id)
                                     :parent-id (om/get-state owner :parent-id)
                                     :ops ops})
                      (recur)))
                  ;; incoming messages
                  (go-loop []
                    (let [{:keys [id ops]} (<! (:inbound queue))]
                      (apply-operation! (get-in app [:editor :document-tree]) queue ops)
                      (write-parent-id! owner id))
                    (recur))
                  ;; roundtrip ack
                  (go-loop []
                    (let [{:keys [server-id]} (<! (:recv-ids queue))]
                      (write-parent-id! owner server-id)
                      (recur)))))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
                (let [current-user (get-in app [:editor :authors :current-user])]
                  (when-let [author (get-in app [:editor :authors :cursors current-user])]
                    (caret-position (:node author) (:offset author)))))
    om/IRenderState
    (render-state [_ {:keys [update]}]
                  (apply dom/div #js {:onClick #(track-author-cursor! % (get-in app [:editor :authors]))}
                         (into (:rendered-nodes
                                (reduce (fn [{:keys [offset] :as out} node]
                                          (let [indexed-node (assoc node :start-offset offset)
                                                comp (component-for-node (:node-type node))]
                                            (-> out
                                                (update-in [:offset] + (:length node))
                                                (update-in [:rendered-nodes] conj
                                                           (om/build comp indexed-node {:key :id
                                                                                        :init-state {:update update}})))))
                                        {:offset 0 :rendered-nodes []}
                                        (get-in app [:editor :document-tree])))
                               (when-let [cursors (vals (get-in app [:editor :authors :cursors]))]
                                 (om/build-all author-cursor cursors)))))))
