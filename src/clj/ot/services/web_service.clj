(ns ot.services.web-service
  (:use [compojure.core :only [GET]])
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [ot.core.web-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(defprotocol WebService)

(tk/defservice web-service
  WebService
  [[:ConfigService get-in-config]
   [:WebsocketService add-ring-handler]
   [:DocumentService submit-request request-document]]
  (init [this context]
        (log/info "Initializing WebService")
        (let [url-prefix (get-in-config [:web :url-prefix])
              context-app (compojure/context url-prefix [] core/editor-routes)]
          (add-ring-handler (compojure/routes
                             (GET "/editor/documents/:id.json" [id]
                                  (let [uuid (java.util.UUID/fromString id)
                                        text (request-document uuid)]
                                    (core/respond-with-doc id text 1)))))
          (add-ring-handler context-app)
          (add-ring-handler core/app-routes)
          context))
  (start [this context]
         (log/info "Starting WebService")
         (core/handle-connections submit-request)
         context)
  (stop [this context]
        (core/shutdown)
        context))
