(ns ot.routes.router
  (:use [compojure.core :only [defroutes GET POST]]
        [org.httpkit.server])
  (:require [ot.controllers.app :as app]
            [compojure.route :as route]
            [ot.templating.views :as views]))

(defroutes app-routes
  (GET "/" [] (views/home-page))
  (POST "/live" [] app/live-handler)
  (GET "/ws" [] app/async-handler)
  (route/resources "/")
  (route/not-found "Not Found"))
