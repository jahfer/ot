(ns ot.http.server
  (:use [compojure.handler :only [site]]
        [ring.middleware.edn]
        [org.httpkit.server])
  (:require [ring.middleware.reload :as reload]
            [ot.routes.router :as routes]
            [clojure.tools.logging :as log]))


(def in-dev? true)

(def app
  (-> routes/app-routes
      wrap-edn-params))

(defn start-server [port]
  (log/info "Initializing HTTP server")
  (let [handler (if in-dev?
                  (reload/wrap-reload (site #'app))
                  (site #'app))]
    (run-server handler {:port port})
    (log/info "Server is running...")))
