(ns ot.services.operational-transform-service
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [ot.core.operational-transform-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(defprotocol OperationalTransformService)

(tk/defservice operational-transform-service
  OperationalTransformService
  [[:ConfigService get-in-config]
   [:WebsocketService add-ring-handler]]
  (init [this context]
        (log/info "Initializing operational transform service")
        (let [url-prefix (get-in-config [:editor-web :url-prefix])
              context-app (compojure/context url-prefix [] core/editor-routes)]
          (add-ring-handler context-app)
          (add-ring-handler core/app-routes)
          (assoc context
            :url-prefix url-prefix
            :document {:body "Hai"
                       :version 0})))
  (start [this context]
         (log/info "Starting operational transform service")
         (core/handle-connections)
         context)
  (stop [this context]
        (core/shutdown)
        context))
