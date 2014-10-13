(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor :as editor]
            [ot.lib.repl :as repl])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [jayq.macros :only [ready let-ajax]]))

(enable-console-print!)

;; Define intial state of app
(def app-state (atom {:editor {:id []
                               :parent-id []
                               :owned-ids []
                               :input {:caret 0
                                       :text nil}}}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "container"}
                     (dom/header nil
                                 (dom/h1 #js {:className "page-title"} "Editor"))
                     (om/build editor/editor (:editor app))))))

(defn main [target state]
  (om/root ot-app state {:target target}))

(defn setup! []
  (let-ajax [remote-doc {:url "/editor/documents/1"}]
            (swap! app-state assoc-in [:editor :input :text] (:doc remote-doc))
            (swap! app-state assoc-in [:editor :id] [(:tx-id remote-doc)])
            (main (sel1 :#app) app-state)))

(ready [] (setup!))
