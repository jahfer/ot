(ns ot.core.editor-web-core
  (:require [clojure.tools.logging :as log]
            [compojure.core :as compojure]
            [compojure.route :as route]))

(defn app
  []
  (compojure/routes
   (compojure/GET "/:caller" [caller]
                  (fn [req]
                    (log/info "Handling request for caller:" caller)
                    {:status 200
                     :headers {"Content-Type" "text/plain"}
                     :body (str "Hello" caller)}))
   (route/not-found "Not found")))
