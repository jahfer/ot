(ns ot.components.node-editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [othello.documents :as documents]))

(defn text-node [data]
  (reify
    om/IRender
    (render [this]
      (dom/span nil data))))

(defn link-node [data]
  (reify
    om/IRender
    (render [this]
      (dom/a #js {:alt (:alt data) :href (:href data)}
             (:text data)))))

(defn node-editor [app-owner]
  (reify
    om/IDisplayName (display-name [_] "NodeEditor")
    om/IInitState
    (init-state [this]
      {:document-tree [{:nodeType ::documents/text :data "Hello, "}
                       {:nodeType ::link :data {:href "http://jahfer.com"
                                                :text "Jahfer"
                                                :alt "Jahfer Husain's Portfolio"}}
                       {:nodeType ::documents/text :data "!"}]})
    om/IRenderState
    (render-state [this state]
      (dom/div nil
               (map (fn [node]
                      (case (:nodeType node)
                        ::documents/text (om/build text-node (:data node))
                        ::link (om/build link-node (:data node))))
                    (:document-tree state))))))
