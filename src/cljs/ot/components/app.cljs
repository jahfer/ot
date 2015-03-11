(ns ot.components.app
  (:require [ot.components.headers :as headers]
            [ot.components.documents :as documents]
            [om.core :as om :include-macros true]
            [om.dom :as dom]))

(defn dominant-component [app-state owner]
  (condp = (:navigation-point app-state)
    :document-index documents/document-index
    :document-edit  documents/document-container
    :document-show  documents/document-container))

(defn app [app owner opts]
  (reify
    om/IDisplayName (display-name [_] "App")
    om/IRender
    (render [_]
      (dom/div #js {:className "container"}
               (om/build headers/default-header app)
               (if-not (:navigation-point app)
                 (dom/img #js {:className "loading" :src "/img/loading.gif" :alt "Loading..."})
                 (let [com (dominant-component app owner)]
                   (om/build com app)))))))
