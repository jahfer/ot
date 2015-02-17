(ns ot.components.app
  (:require [ot.components.editor :as editor]
            [ot.components.doc-render :as doc-render]
            [om.core :as om :include-macros true]
            [om.dom :as dom]))

(defn dominant-component [app-state owner]
  (condp = (get-in app-state [:navigation-point])
    :document-edit editor/editor-com
    :document-show doc-render/doc-render-com))

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
