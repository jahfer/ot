(ns ot.components.node-editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.lib.util :as util]
            [cljs.core.async :refer [chan <! put!]]
            [othello.documents :as documents]
            [othello.operations :as operations])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(def current-user :123)

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
      (println "Pressed" char "@" op-offset)
      (let [ops (operations/oplist
                  ::operations/ret op-offset
                  ::operations/ins char
                  ;; should retain over the length of the document!!
                  ::operations/ret (- (:length node) caret))
            update-ch (om/get-state owner :update)]
        (put! update-ch {:type :insert :ops ops :node node})
        (om/transact! node :length inc)
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
                ::operations/ret (- (:length node) caret))
          update-ch (om/get-state owner :update)]
      (println "Pressed DELETE @" (+ caret (:start-offset node)))
      (put! update-ch {:type :delete :ops ops :node node})
      (om/transact! node :length dec)
      (om/transact! node [:authors current-user :offset] dec))
    (.preventDefault e)))

(defn track-author-cursor [e authors]
  (om/transact! authors :cursors
                #(assoc % current-user {:id 123
                                        :name "Jahfer"
                                        :node (.-target e)
                                        :offset (caret-position)})))

(defn author-cursor [author _]
  (reify
    om/IDisplayName (display-name [_] "CursorNode")
    om/IRender
    (render [_]
      (let [range (.createRange js/document)
            inner-node (aget (.-childNodes (:node author)) 0)]
        (.setStart range inner-node (:offset author))
        (.setEnd range inner-node (inc (:offset author)))
        (let [bounding (.getBoundingClientRect range)]
          (dom/div #js {:className "author-cursor"
                        :style #js {:top (str (.-top bounding) "px")
                                    :left (str (.-left bounding) "px")
                                    :height "18px"}}
                   (dom/span #js {:className "author"} (:name author))))))))

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

(defn node-editor [editor owner]
  (reify
    om/IDisplayName (display-name [_] "NodeEditor")
    om/IInitState
    (init-state [_]
      {:update (chan)})
    om/IWillMount
    (will-mount [_]
      (let [update (om/get-state owner :update)]
        (go-loop []
                 (let [{:keys [node type ops]} (<! update)
                       relative-ops (update-in ops [0 :val] #(- % (:start-offset node)))
                       current-user (get-in editor [:authors :current-user])]
                   (om/transact! node :data #(documents/apply-ops % relative-ops))
                   (condp = type
                         :delete (om/transact! editor [:authors :cursors current-user :offset] dec)
                         :insert (om/transact! editor [:authors :cursors current-user :offset] inc)) ;; inc or dec
                   (recur)))))
    om/IDidUpdate
    (did-update [this prev-props prev-state]
      (let [current-user (get-in editor [:authors :current-user])]
        (when-let [author (get-in editor [:authors :cursors current-user])]
          (caret-position (:node author) (:offset author)))))
    om/IRenderState
    (render-state [_ {:keys [update]}]
      (apply dom/div #js {:onClick #(track-author-cursor % (:authors editor))}
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
                             (:document-tree editor)))
                   (when-let [cursors (vals (get-in editor [:authors :cursors]))]
                     (om/build-all author-cursor cursors)))))))
