(ns ot.core.editor-web-core
  (:use [compojure.core :only [defroutes GET]]
        org.httpkit.server)
  (:import [java.io ByteArrayInputStream])
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [compojure.route :as route]
            [cognitect.transit :as transit]
            [digest]
            [ot.templating.views :as views]
            [ot.transforms :refer :all]
            [ot.documents :as documents]
            [ot.transit-handlers :as transit-handlers]
            [clojure.core.async :refer [go put! <! chan]]))

(declare async-handler)

(def root-document (atom "Hullo"))
(def history (atom []))

(defn transaction-id [text]
  (digest/md5 text))

(defroutes editor-routes
  (GET "/" [] (views/home-page))
  (GET "/documents/:id" [] (fn [{params :params}]
                             {:status 200
                              :headers {"Content-Type" "application/edn"}
                              :body (pr-str {:id    (:id params)
                                             :doc   @root-document
                                             :tx-id (transaction-id @root-document)})}))
  (GET "/ws" [] async-handler)
  (GET "/test" [] (views/iframed-test)))

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
            in (ByteArrayInputStream. (.getBytes data))
            reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
            parsed-data (transit/read reader)]

        (log/info "[Received]")
        (clojure.pprint/pprint parsed-data)

        (println (->> (repeat "-") (take 30) clojure.string/join))
        (swap! root-document documents/apply-ops (:ops parsed-data))
        (println @root-document)
        (println (->> (repeat "-") (take 30) clojure.string/join))

        (let [{:keys [id ops] :as data} parsed-data]
          (swap! history conj data)
          (clojure.pprint/print-table [:id :parent-id :ops]
                                      (mapv (fn [data]
                                              (update-in data [:ops] (fn [o]
                                                                       (mapv (fn [{:keys [type val]}] {type val}) o))))
                                            @history)))
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