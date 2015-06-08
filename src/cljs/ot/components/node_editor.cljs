(ns ot.components.node-editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.lib.util :as util]
            [cljs.core.async :refer [chan <! put!]]
            [othello.documents :as documents]
            [othello.operations :as operations])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defn caret-position
  "gets or sets the current cursor position"
  ([]
   (util/toInt (.-startOffset (.getRangeAt (.getSelection js/window) 0))))
  ([owner new-pos]
   (let [editor (om/get-node owner)]
     (.setSelectionRange editor new-pos new-pos))))

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
            update (om/get-state owner :update)]
        (put! update {:ops ops :node node})
        (om/transact! node :length inc)))
    (.preventDefault e)))

(defn handle-keydown [e node owner]
  (when (util/in? [8 46] (.-which e))
    (let [caret (caret-position)]
      (println "Pressed DELETE @" (+ caret (:start-offset node)))
      (om/transact! node :length dec))
    (.preventDefault e)))

(defn render-author-cursor [e node owner]
  (om/transact! node :authors #(assoc % :123 {:id 123
                                              :name "Jahfer"
                                              :node (aget (.-childNodes (om/get-node owner "node-content")) 0)
                                              :offset (caret-position)})))

(defn author-cursor [cursor owner]
  (reify
    om/IDisplayName (display-name [_] "CursorNode")
    om/IRender
    (render [this]
      (let [range (.createRange js/document)]
        (.setStart range (:node cursor) (:offset cursor))
        (.setEnd range (:node cursor) (inc (:offset cursor)))
        (let [bounding (.getBoundingClientRect range)]
          (.log js/console bounding)
          (dom/div #js {:className "author-cursor"
                        :style #js {:top (str (.-top bounding) "px")
                                    :left (str (.-left bounding) "px")
                                    :height "18px"}}
                   (dom/span #js {:className "author"} (:name cursor))))))))

(defn text-node [node owner]
  (reify
    om/IDisplayName (display-name [_] "TextNode")
    om/IRender
    (render [this]
      (dom/span nil
                (apply dom/span nil
                       ;; need to pass (om/get-node owner here, if possible)
                       (om/build-all author-cursor (vals (:authors node))))
                (dom/span #js {:ref "node-content"
                               :contentEditable true
                               :onKeyPress #(handle-keypress % node owner)
                               :onKeyDown #(handle-keydown % node owner)
                               :onClick #(render-author-cursor % node owner)}
                         (:data node))))))

(defn link-node [node owner]
  (reify
    om/IDisplayName (display-name [_] "LinkNode")
    om/IRender
    (render [this]
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
                offset (:val (first ops))
                relative-ops (update-in ops [0 :val] #(- % (:start-offset node)))]
            (om/transact! node :data #(documents/apply-ops % relative-ops))
            (recur)))))
    om/IRenderState
    (render-state [this {:keys [update]}]
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
