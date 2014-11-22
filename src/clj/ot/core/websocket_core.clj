(ns ot.core.websocket-core
  (:use [org.httpkit.server]
        [ring.middleware.params])
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]))

(def ring-handlers (atom {}))

(defn params-thru-middleware []
  (-> @ring-handlers
      wrap-params))

(defn start-server [port]
  (run-server (params-thru-middleware) {:port port}))

(defn add-handlers [handlers]
  (swap! ring-handlers compojure/routes handlers))
