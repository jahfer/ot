(ns ot.cljs.components.editor.input
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.cljs.lib.util :as util]
            [ot.crossover.transforms :as transforms]
            [cljs.core.async :refer [put! chan <!]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(defn gen-insert-op [key owner]
  (let [caret-loc (om/get-state owner :caret)
        retain-before (transforms/->Op :ret caret-loc)
        insert (transforms/->Op :ins key)
        chars-remaining (- (om/get-state owner :text-length) caret-loc)
        retain-after (transforms/->Op :ret chars-remaining)]
    [retain-before insert retain-after]))

(defn handle-keypress [e input owner]
  (om/set-state! owner :caret (caret-position))
  (let [key (util/keyFromCode (.-which e))
        operations (gen-insert-op key owner)]
    (.log js/console (pr-str operations))
    (put! input operations)))

(defn caret-position
  "gets or sets the current cursor position"
  ([]
   (util/toInt (.-selectionStart (sel1 :#editor))))
  ([new-pos]
   (let [editor (sel1 :#editor)]
     (.setSelectionRange editor new-pos new-pos))))

(defn editor-input [app owner {:keys [text] :as opts}]
  (reify
    om/IInitState
    (init-state [_]
                {:caret 0})
    om/IWillUpdate
    (will-update [this next-props next-state])
    om/IDidUpdate
    (did-update [this prev-props prev-state]
                (caret-position (om/get-state owner :caret)))
    om/IRenderState
    (render-state [this {:keys [input caret]}]
                  (dom/textarea #js {:id "editor"
                                     :value text
                                     :onKeyPress #(handle-keypress % input owner)}))))
