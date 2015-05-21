(ns ot.components.node-editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.lib.util :as util]
            [othello.documents :as documents]))

(enable-console-print!)

(defn caret-position
  "gets or sets the current cursor position"
  ([]
   (util/toInt (.-startOffset (.getRangeAt (.getSelection js/window) 0))))
  ([owner new-pos]
   (let [editor (om/get-node owner)]
     (.setSelectionRange editor new-pos new-pos))))

(defn handle-keypress [e node]
  (when (not (util/in? [8 37 38 39 40] (.-keyCode e)))
    (let [key (util/keyFromCode (.-which e))
          caret (caret-position)]
      (println "Pressed" key "@" (+ caret (:start-offset node)))
      (om/transact! node :length inc))
    (.preventDefault e)))

(defn handle-keydown [e node]
  (when (= 8 (.-which e))
    (let [caret (caret-position)]
      (println "Pressed DELETE @" (+ caret (:start-offset node)))
      (om/transact! node :length dec))
    (.preventDefault e)))

(defn text-node [node owner]
  (reify
    om/IDisplayName (display-name [_] "TextNode")
    om/IRender
    (render [this]
      (dom/span #js {:contentEditable true
                     :onKeyPress #(handle-keypress % node)
                     :onKeyDown #(handle-keydown % node)}
                (:data node)))))

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
    om/IRender
    (render [this]
      (apply dom/div nil
               (:rendered-nodes
                (reduce (fn [{:keys [offset] :as out} node]
                          (let [indexed-node (assoc node :start-offset offset)
                                comp (component-for-node (:node-type node))]
                            (-> out
                                (update-in [:offset] + (:length node))
                                (update-in [:rendered-nodes] conj
                                           (om/build comp indexed-node {:key :id})))))
                        {:offset 0 :rendered-nodes []}
                        (:document-tree editor)))))))
