(ns ot.services.websocket-service
  (:use [compojure.handler :only [site]])
  (:require [clojure.tools.logging :as log]
            [ot.core.websocket-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))


(defprotocol WebsocketService
  (set-routes [this routes]))

(def app (atom {}))

(tk/defservice websocket-service
               WebsocketService
               [[:ConfigService get-in-config]]
               (init [this context]
                     (log/info "Initializing websocket service")
                     (assoc context :websocket-server (core/initialize-context)))
               (set-routes [this routes]
                             (reset! app routes))
               (start [this context]
                      (log/info "Starting websocket service" context)
                      (let [port (get-in-config [:websocket :port])]
                        (core/start-server @app port))
                      context))
