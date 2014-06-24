(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor :as editor]
            [ot.lib.repl :as repl])
  (:use-macros [dommy.macros :only [node sel sel1]]))

;; Define intial state of app
(def app-state (atom {:editor {:id [0]
                               :owned-ids []
                               :input {:caret 0
                                       :text "Hello"}}}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "container"}
                     (dom/header nil
                                 (dom/h1 #js {:className "page-title"} "Editor"))
                     (om/build editor/editor (:editor app))))))

;; Let's kick things off
(om/root ot-app app-state
         {:target (sel1 :#app)})
