(ns ot.cljs.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.cljs.components.editor :as editor])
  (:use-macros [dommy.macros :only [node sel sel1]]))

;; Define intial state of app
(def app-state (atom {}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IRender
    (render [_]
            (om/build editor/editor app))))

;; Let's kick things off
(om/root ot-app app-state
         {:target (sel1 :#app)})
