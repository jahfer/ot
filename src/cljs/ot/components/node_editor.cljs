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
        (put! update-ch {:ops ops :node node})
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
      (put! update-ch {:ops ops :node node})
      (om/transact! node :length dec)
      (om/transact! node [:authors current-user :offset] dec))
    (.preventDefault e)))

(defn render-author-cursor [_ node owner]
  (om/transact! node :authors #(assoc % current-user {:id 123
                                                      :name "Jahfer"
                                                      :node (om/get-node owner "node-content")
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
    om/IDidUpdate
    (did-update [_ _ _]
      (let [dom-node (om/get-node owner "node-content")]
        (when-let [author (get (:authors node) current-user)]
          (caret-position dom-node (:offset author)))))
    om/IRender
    (render [_]
      (dom/span nil
                (apply dom/span nil
                       (om/build-all author-cursor (vals (:authors node))))
                (dom/span #js {:ref "node-content"
                               :contentEditable true
                               :onKeyPress #(handle-keypress % node owner)
                               :onKeyDown #(handle-keydown % node owner)
                               :onClick #(render-author-cursor % node owner)}
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
          (let [{:keys [node ops]} (<! update)
                relative-ops (update-in ops [0 :val] #(- % (:start-offset node)))]
            (om/transact! node :data #(documents/apply-ops % relative-ops))
            (recur)))))
    om/IRenderState
    (render-state [_ {:keys [update]}]
      (apply dom/div nil
               (:rendered-nodes
                (reduce (fn [{:keys [offset] :as out} node]
                          (let [indexed-node (assoc node :start-offset offset)
                                comp (component-for-node (:node-type node))]
                            (-> out
                                (update-in [:offset] + (:length node))
                                (update-in [:rendered-nodes] conj
                                           (om/build comp indexed-node {:key :id
                                                                        :init-state {:update update}})))))
                        {:offset 0 :rendered-nodes []}
                        (:document-tree editor)))))))
