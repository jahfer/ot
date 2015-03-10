(ns ot.components.app
  (:require [ot.components.documents :as documents]
            [om.core :as om :include-macros true]
            [om.dom :as dom]))

(defn dominant-component [app-state owner]
  (condp = (get-in app-state [:navigation-point])
    :document-index documents/document-index
    :document-edit documents/document-container
    :document-show documents/document-container))

(defn app [app owner opts]
  (reify
    om/IDisplayName (display-name [_] "App")
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
               (dom/header nil
                           (dom/h1 #js {:className "page-title"} "Editor"))
               (if-not (:navigation-point app)
                 (dom/div nil "Whoops!")
                 (let [com (dominant-component app owner)]
                   (om/build com app)))))))
