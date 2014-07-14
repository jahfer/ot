(ns ot.core.editor-web-core
  (:use [compojure.core :only [defroutes GET]]
        org.httpkit.server)
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [compojure.route :as route]
            [ot.templating.views :as views]
            [ot.transforms :refer :all]
            [ot.documents :as documents]
            [clojure.core.async :refer [go put! <! chan]]))

(declare async-handler)

(def root-document (atom "Hullo"))

(defroutes editor-routes
  (GET "/" [] (views/home-page))
  (GET "/documents/:id" [] (fn [{params :params}]
                             {:status 200
                              :headers {"Content-Type" "application/edn"}
                              :body (pr-str {:id (:id params) :doc @root-document})}))
  (GET "/ws" [] async-handler))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not found"))

(def input (chan))
(def clients (atom {}))

(declare broadcast)

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(defn handle-connections []
  (go
    (while true
      (let [data (<! input)
            parsed-data (edn/read-string
                         {:readers {'ot.transforms.Op ot.transforms/map->Op}}
                         data)]
        (println "Received from client:")
        (clojure.pprint/pprint parsed-data)
        (println (take 30 (repeat "-")))
        (swap! root-document documents/apply-ops (:ops parsed-data))
        (println @root-document)
        (println (take 30 (repeat "-")))
        (broadcast data)))))

(defn shutdown []
  (doseq [client @clients]
    (close (key client))
    (swap! clients dissoc client))
  (log/info "Finished closing of websocket clients"))

(defn broadcast [msg]
  (log/debug "emitting message to client" msg)
  (Thread/sleep 2000)
  (doseq [client @clients]
    (send! (key client) msg)))
