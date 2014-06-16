(ns ot.core.websocket-core
  (:use [org.httpkit.server])
  (:require [clojure.tools.logging :as log]))

(defn start-server [handler port]
  (run-server handler {:port port}))

(defn initialize-context []
  {:handlers []
   :server nil})
