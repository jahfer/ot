(ns ot.main
  (:use [compojure.core]
        [ring.middleware.edn]
        [compojure.handler :only [site]]
        [org.httpkit.server])
  (:require [ot.views :as views]
            [compojure.route :as route]
            [ring.middleware.reload :as reload]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (GET "/" []
       (views/home-page))
  (POST "/live" [type val]
        (println (str "type: " type " val: " val))
        (generate-response {:hello type}))
  (GET "/ws" req
       (with-channel req ch
         (println "New WebSocket channel:" ch)
         (on-close ch (fn [status] (println "channel closed: " status)))
         (on-receive ch (fn [data]
                          (send! ch data)))))
  (route/resources "/")
  (route/not-found "Not Found"))

(def in-dev? true)

(def app
  (-> app-routes
      wrap-edn-params))

(defn -main [& args]
  (let [handler (if in-dev?
                  (reload/wrap-reload (site #'app))
                  (site #'app))]
    (run-server handler {:port 3000})
    (println "Server is running...")))
