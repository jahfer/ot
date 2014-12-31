(ns ot.core.operational-transform-core
  (:use [compojure.core :only [defroutes GET]])
  (:import [java.io ByteArrayInputStream]
           [java.io ByteArrayOutputStream])
  (:require [clojure.tools.logging :as log]
            [org.httpkit.server :as httpkit]
            [compojure.route :as route]
            [cognitect.transit :as transit]
            [ot.templating.views :as views]
            [ot.transforms :refer :all]
            [ot.documents :as documents]
            [ot.composers :as composers]
            [ot.operations :as operations]
            [ot.core.document-core :as document-core]
            [ot.transit-handlers :as transit-handlers]
            [clojure.core.async :refer [go-loop put! <! chan]]))

(declare async-handler)
(declare broadcast)

(def input (chan))
(def clients (atom {}))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not found"))

(defroutes editor-routes
  (GET "/" [] (views/home-page))
  (GET "/documents/:id" [] (fn [{params :params}]
                             {:status 200
                              :headers {"Content-Type" "application/edn"}
                              :body (pr-str {:id      (:id params)
                                             :doc     (deref document-core/root-document)
                                             :version (deref document-core/doc-version)})}))
  (GET "/ws" [] async-handler)
  (GET "/iframe" [] (views/iframed-test)))

(defn- write-message [msg]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json {:handlers transit-handlers/write-handlers})]
    (transit/write writer msg)
    (.toString out)))

(defn async-handler [req]
  (httpkit/with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (httpkit/on-receive ch (fn [data]
                     (put! input data)))
    (httpkit/on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(defn handle-connections []
  (go-loop []
    (let [raw-data (<! input)
          in (ByteArrayInputStream. (.getBytes raw-data))
          reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
          data (transit/read reader)
          cleaned-data (document-core/submit-request data)]
      (if cleaned-data
        (broadcast (write-message cleaned-data))
        (log/error "Failed to process request of data"))      
      (recur))))

(defn shutdown []
  (doseq [client @clients]
    (httpkit/close (key client))
    (swap! clients dissoc client))
  (log/info "Finished closing of websocket clients"))

(defn broadcast [msg]
  (Thread/sleep 1000)
  (doseq [client @clients]
    (httpkit/send! (key client) msg)))
