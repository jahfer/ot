(ns ot.handler
  (:use compojure.core)
  (:require [ot.views :as views]
            [compojure.handler :as handler]
            [compojure.route :as route]))

(defroutes app-routes
  (GET "/" []
       (views/home-page))
  (GET "/edit" []
       (views/edit-page))
  (POST "/live" {params :params}
        (views/live-update params))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))