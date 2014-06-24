(ns ot.core.websocket-core
  (:use [org.httpkit.server])
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]))

(def ring-handlers (atom {}))

(defn start-server [port]
  (run-server @ring-handlers {:port port}))

(defn add-handlers [handlers]
  (swap! ring-handlers compojure/routes handlers))
