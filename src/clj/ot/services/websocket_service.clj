(ns ot.services.websocket-service
  (:use [compojure.handler :only [site]])
  (:require [clojure.tools.logging :as log]
            [ot.core.websocket-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]))

(defprotocol WebsocketService
  (add-ring-handler [this handlers]))

(tk/defservice websocket-service
  WebsocketService
  [[:ConfigService get-in-config]]
  (add-ring-handler [this handlers] (core/add-handlers handlers))
  (init [this context]
        (assoc context :server (atom nil)))
  (start [this context]
         (let [port (get-in-config [:websocket :port])
               server (:server context)]
           (reset! server (core/start-server port)))
         context)
  (stop [this context]
        (let [server (:server context)]
          (reset! server nil))
        context))
