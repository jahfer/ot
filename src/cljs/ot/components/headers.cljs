(ns ot.components.headers
  (:require [ot.routes :as routes]
            [om.core :as om :include-macros true]
            [om.dom :as dom]))

(defn default-header [app owner]
  (reify
    om/IDisplayName (display-name [_] "DefaultHeader")
    om/IRender
    (render [_]
      (dom/header nil
                  (dom/h1 #js {:className "page-title"} "Editor")
                  (when-not (= (:navigation-point app) :document-index)
                    (dom/a #js {:href (routes/documents-path)} "See all documents"))))))
