(ns ot.http.server
  (:use [compojure.handler :only [site]]
        [ring.middleware.edn]
        [org.httpkit.server])
  (:require [ring.middleware.reload :as reload]
            [ot.routes.router :as routes]))


(def in-dev? true)

(def app
  (-> routes/app-routes
      wrap-edn-params))

(defn start-server [port]
  (let [handler (if in-dev?
                  (reload/wrap-reload (site #'app))
                  (site #'app))]
    (run-server handler {:port port})
    (println "Server is running...")))
