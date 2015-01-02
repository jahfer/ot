(ns ot.services.web-service
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [ot.core.web-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(defprotocol WebService)

(tk/defservice web-service
  WebService
  [[:ConfigService get-in-config]
   [:WebsocketService add-ring-handler]
   [:DocumentStorageService insert select]]
  (init [this context]
        (log/info "Initializing WebService")
        (let [url-prefix (get-in-config [:web :url-prefix])
              context-app (compojure/context url-prefix [] core/editor-routes)]
          (add-ring-handler context-app)
          (add-ring-handler core/app-routes)
          (assoc context
            :url-prefix url-prefix
            :document {:body "Hai"
                       :version 0})))
  (start [this context]
         (log/info "Starting WebService")
         (core/handle-connections)
         context)
  (stop [this context]
        (core/shutdown)
        context))
