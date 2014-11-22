(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor :as editor]
            [ot.lib.repl :as repl])
  (:use-macros [jayq.macros :only [ready let-ajax]]))

(enable-console-print!)

;; Define intial state of app
(def app-state (atom {:editor {:local-id []
                               :parent-id []
                               :owned-ids []
                               :text ""}}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
               (dom/header nil
                           (dom/h1 #js {:className "page-title"} "Editor"))
               (om/build editor/editor-view (:editor app)
                         {:init-state {:text (get-in app [:editor :text])
                                       :parent-id (get-in app [:editor :parent-id 0])}})))))

(defn main [target state]
  (om/root ot-app state {:target target}))

(defn setup! []
  (let-ajax [remote-doc {:url "/editor/documents/1"}]
            (swap! app-state assoc-in [:editor :text] (:doc remote-doc))
            (swap! app-state assoc-in [:editor :parent-id] [(:version remote-doc)])
            (main (.getElementById js/document "app") app-state)))

(ready [] (setup!))
