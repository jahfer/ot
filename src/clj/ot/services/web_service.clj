(ns ot.services.web-service
  (:use [compojure.core :only [GET]])
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [clojure.core.match :refer [match]]
            [ot.core.web-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(defprotocol WebService)

(tk/defservice web-service
  WebService
  [[:ConfigService get-in-config]
   [:WebsocketService add-ring-handler]
   [:DocumentService submit-request request-document request-documents]]
  (init [this context]
        (log/debug "Initializing WebService")
        (let [url-prefix (get-in-config [:web :url-prefix])
              context-app (compojure/context url-prefix [] core/editor-routes)]
          (add-ring-handler (compojure/routes
                             (GET "/editor/documents/:id.json" [id]
                                  (let [uuid (java.util.UUID/fromString id)]
                                    (match (request-document uuid)
                                           [:text text :deltaid deltaid] (core/edn-response {:id id
                                                                                             :text text
                                                                                             :deltaid deltaid})
                                           :else (core/not-found
                                                  (str "Document " id " not found")))))
                             (GET "/editor/documents.json" [id]
                                  (core/edn-response {:documents (request-documents)}))))
          (add-ring-handler context-app)
          (add-ring-handler core/app-routes)
          context))
  (start [this context]
         (log/info "WebService started")
         (core/handle-connections submit-request)
         context)
  (stop [this context]
        (core/shutdown)
        context))
