(ns ot.components.documents
  (:require [ot.routes :as routes]
            [ot.components.editor :as editor]
            [ot.components.node-editor :as node-editor]
            [om.core :as om :include-macros true]
            [om.dom :as dom])
  (:require-macros [jayq.macros :refer [let-ajax]]
                   [om.core :refer [component]]))

(enable-console-print!)

(defn read-only [{:keys [documentid text]}]
  (reify
    om/IDisplayName (display-name [_] "ReadOnlyDocument")
    om/IRender
    (render [_]
      (dom/div #js {:className "document"}
               (dom/div nil text)
               (dom/a #js
                      {:href (routes/document-edit-path {:id documentid})}
                      "Edit")))))

(defn document-list [documents]
  (reify
    om/IDisplayName (display-name [_] "DocumentList")
    om/IRender
    (render [_]
      (apply dom/ul nil
             (map #(dom/li nil
                           (dom/a #js
                                  {:href (routes/document-path {:id (:documentid %)})}
                                  (:documentid %)))
                  documents)))))

(defn document-index [app owner]
  (reify
    om/IDisplayName (display-name [_] "DocumentIndex")
    om/IWillMount
    (will-mount [this]
      (let-ajax [documents {:url (str (routes/documents-path) ".json")}]
                (om/update! app [:documents] (:documents documents))))
    om/IRender
    (render [_]
      (om/build document-list (:documents app)))))

(defn document-container [app owner]
  (reify
    om/IDisplayName (display-name [_] "DocumentContainer")
    om/IWillMount
    (will-mount [this]
      (let [documentid (get-in app [:navigation-data :documentid])]
        (om/update! app [:documentid] documentid)
        (let-ajax [remote-doc {:url (str (routes/document-path {:id documentid}) ".json")}]
                  (om/update! app [:text] (:text remote-doc)))))
    om/IRender
    (render [_]
      (if (get-in app [:navigation-data :editable])
        (om/build node-editor/node-editor app)  ;;(om/build editor/editor-com app)
        (om/build read-only app)))))
