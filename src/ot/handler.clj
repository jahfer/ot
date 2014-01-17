(ns ot.handler
  (:use compojure.core)
  (:use ring.middleware.edn)
  (:require [ot.views :as views]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (GET "/" []
       (views/home-page))
  (GET "/edit" []
       (views/edit-page))
  (POST "/live" [type val]
        (println (str "type: " type " val: " val))
        (generate-response {:hello type}))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-edn-params))
