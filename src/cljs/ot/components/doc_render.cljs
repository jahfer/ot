(ns ot.components.doc-render
  (:require [ot.routes :as routes]
            [om.core :as om :include-macros true]
            [om.dom :as dom])
  (:require-macros [jayq.macros :refer [let-ajax]]))

(defn doc-render-com [app owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (let [documentid (get-in app [:navigation-data :documentid])]
        (let-ajax [remote-doc {:url (str (routes/document-path {:id documentid}) ".json")}]
                  (om/update! app [:text] (:doc remote-doc)))))
    om/IRender
    (render [_]
      (dom/div #js {:className "doc-render"} (:text app)))))
