(ns ot.services.editor-web-service
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [ot.core.editor-web-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(tk/defservice editor-web-service
  [[:ConfigService get-in-config]
   [:WebsocketService add-ring-handler]]
  (init [this context]
        (log/info "Initializing editor webservice")
        (let [url-prefix (get-in-config [:editor-web :url-prefix])
              context-app (compojure/context url-prefix [] core/editor-routes)]
          (add-ring-handler context-app)
          (add-ring-handler core/app-routes)
          (assoc context :url-prefix url-prefix)))
  (start [this context]
         (log/info "Starting editor webservice")
         (core/handle-connections)
         context)
  (stop [this context]
        (core/shutdown)
        context))
