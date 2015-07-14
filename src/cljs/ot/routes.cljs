(ns ot.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [cljs.core.async :refer [put!]]))

(enable-console-print!)

(defn open! [nav-ch navigation-point args]
  (put! nav-ch [navigation-point args]))

(defn define-document-routes! [nav-ch {:keys [debug] :or {:debug false}}]
  (defroute document-edit-path "/editor/documents/:id/edit" [id]
    (open! nav-ch :document-edit {:documentid id :editable true :debug debug}))
  (defroute document-path "/editor/documents/:id" [id]
    (open! nav-ch :document-show {:documentid id :editable false :debug debug}))
  (defroute documents-path "/editor/documents" []
    (open! nav-ch :document-index {:debug debug})))

(defn define-routes! [state opts]
  (let [nav-ch (get-in @state [:comms :nav])]
    (define-document-routes! nav-ch opts)))
