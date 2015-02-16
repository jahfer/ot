(ns ot.core.websocket-core
  (:use [org.httpkit.server]
        [ring.middleware.params])
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]))

(defn params-thru-middleware [routes]
  (-> @routes
      wrap-params))

(defn start-server [routes port]
  (run-server (params-thru-middleware routes) {:port port}))

(defn add-handlers! [routes handlers]
  (swap! routes compojure/routes handlers))
