(ns ot.services.websocket-service
  (:use [compojure.handler :only [site]])
  (:require [clojure.tools.logging :as log]
            [ot.core.websocket-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.services :refer [service-context]]))

(defprotocol WebsocketService
  (add-ring-handler [this handlers]))

(tk/defservice websocket-service
  WebsocketService
  [[:ConfigService get-in-config]]
  (add-ring-handler [this handlers]
                    (let [context (service-context this)]
                      (core/add-handlers! (:routes context) handlers)))
  (init [this context]
        (log/debug "Initializing WebsocketService")
        (-> context
            (assoc :server (atom nil))
            (assoc :routes (atom {}))))
  (start [this context]
         (log/debug "Starting WebsocketService")
         (let [port (get-in-config [:websocket :port])
               server (:server context)]
           (reset! server (core/start-server (:routes context) port)))
         context)
  (stop [this context]
        (let [server (:server context)]
          (@server :timeout 100)
          (reset! server nil))
        context))
