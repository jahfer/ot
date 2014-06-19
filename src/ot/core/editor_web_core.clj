(ns ot.core.editor-web-core
  (:use [compojure.core :only [defroutes GET]])
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [ot.controllers.documents :as documents]
            [ot.templating.views :as views]))

(defroutes app-routes
  (GET "/" [] (views/home-page))
  (GET "/ws" [] documents/async-handler)
  (route/resources "/")
  (route/not-found "Not found"))
