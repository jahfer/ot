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

;; -------

(def rejected-keys [8 37 38 39 40])

(defn handle-keypress [e node]
  (when (not (util/in? rejected-keys (.-keyCode e)))
    (let [key (util/keyFromCode (.-which e))
          caret (caret-position)]
      (println "Pressed" key "@" (+ caret (:startOffset node)))
      ((:update-fn node) e node))
    (.stopPropagation e)
    false))

(defn handle-keydown [e node]
  (when (= 8 (.-which e))
    (let [caret (caret-position)]
      (println "Pressed DELETE @" (+ caret (:startOffset node))))
    (.stopPropagation e)
    false))

;; -------

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
      (let [data (:data node)]
        (dom/a #js {:alt (:alt data) :href (:href data)}
               (:text data))))))

;; -------

(defn on-update [editor]
  (fn [e active-node]
    (let [length (:length active-node)
          nodes-after (drop (inc (:index active-node)) (:document-tree editor))]
      (doall (map #(om/transact! % :startIndex inc) nodes-after)))))

(defn node-editor [editor owner]
  (reify
    om/IDisplayName (display-name [_] "NodeEditor")
    om/IRender
    (render [this]
      (dom/div nil
               (loop [index 0 offset 0 nodes (:document-tree @editor) out []]
                 (if (seq nodes)
                   (let [node (first nodes)
                         indexed-node (-> node
                                          (assoc :index index)
                                          (assoc :startOffset offset)
                                          (assoc :update-fn (on-update editor)))
                         out (conj out (case (:nodeType node)
                                         ::documents/text (om/build text-node indexed-node)
                                         :link (om/build link-node indexed-node)))]
                     (recur (inc index)
                            (+ offset (:length node))
                            (rest nodes)
                            out))
                   out))))))
