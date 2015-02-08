(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.components.editor :as editor]
            [ot.lib.repl :as repl]
            [ot.lib.queue2 :as q2]
            cljsjs.react)
  (:use-macros [jayq.macros :only [ready let-ajax]]))

(enable-console-print!)

;; Define intial state of app
(def app-state (atom {:editor {:local-id []
                               :parent-id []}}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IRender
    (render [_]
      (let [queue (q2/new-queue "ws://localhost:3000/editor/ws")]
        (dom/div #js {:className "container"}
                 (dom/header nil
                             (dom/h1 #js {:className "page-title"} "Editor"))
                 (om/build editor/editor-view (:editor app)
                           {:init-state {:text (get-in app [:editor :text])
                                         :parent-id (get-in app [:editor :parent-id 0])
                                         :queue queue}}))))))

(defn main [target state]
  (om/root ot-app state {:target target}))

(defn setup! []
  (let-ajax [remote-doc {:url "/editor/documents/70ef8740-9237-11e4-aec4-054abea3cfa4.json"}]
            (swap! app-state assoc-in [:editor :text] (:doc remote-doc))
            (swap! app-state assoc-in [:editor :parent-id] [(:version remote-doc)])
            (main (.getElementById js/document "app") app-state)))

(ready [] (setup!))
