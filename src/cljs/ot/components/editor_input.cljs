(ns ot.components.editor-input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.lib.util :as util]
            [ot.operations :as operations]
            [cljs.core.async :refer [put! chan <!]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(enable-console-print!)

(def rejected-keys ["Up" "Down" "Left" "Right" "Backspace"])

(defn caret-position
  "gets or sets the current cursor position"
  ([]
   (util/toInt (.-selectionStart (sel1 :#editor))))
  ([new-pos]
   (let [editor (sel1 :#editor)]
     (.setSelectionRange editor new-pos new-pos))))

(defn gen-insert-op [key cursor]
  (let [caret-loc (:caret @cursor)
        op-list (operations/oplist :ret caret-loc :ins key)
        chars-remaining (- (count (:text @cursor)) caret-loc)]
    (if (zero? chars-remaining)
      op-list
      (let [retain-after (operations/->Op :ret chars-remaining)]
        (conj op-list retain-after)))))

(defn handle-keypress [e cursor comm]
  (when (not (util/in? rejected-keys (.-key e)))
    (om/transact! cursor :caret #(caret-position))
    (let [key (util/keyFromCode (.-which e))
          operations (gen-insert-op key cursor)]
      (put! comm operations)
      (om/transact! cursor :caret inc))))

(defn editor-input [cursor owner opts]
  (reify
    om/IWillUpdate
    (will-update [this next-props next-state])
    om/IDidUpdate
    (did-update [this prev-props prev-state]
                (caret-position (:caret cursor)))
    om/IRenderState
    (render-state [this {:keys [comm]}]
                  (dom/textarea #js {:id "editor"
                                     :value (:text cursor)
                                     :onKeyPress #(handle-keypress % cursor comm)}))))
