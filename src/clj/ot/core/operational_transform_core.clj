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
            [ot.transit-handlers :as transit-handlers]
            [clojure.core.async :refer [go-loop put! <! chan]]))

(declare async-handler)
(declare broadcast)

(def root-document (atom "Hullo"))
(def doc-version (atom 0))
(def input (chan))
(def clients (atom {}))
(def history (atom (sorted-set-by (fn [a b]
                                    (< (:id a) (:id b))))))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not found"))

(defroutes editor-routes
  (GET "/" [] (views/home-page))
  (GET "/documents/:id" [] (fn [{params :params}]
                             {:status 200
                              :headers {"Content-Type" "application/edn"}
                              :body (pr-str {:id      (:id params)
                                             :doc     @root-document
                                             :version @doc-version})}))
  (GET "/ws" [] async-handler)
  (GET "/iframe" [] (views/iframed-test)))

(defn async-handler [req]
  (httpkit/with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (httpkit/on-receive ch (fn [data]
                     (put! input data)))
    (httpkit/on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(defn operations-since-id [id log]
  (map :ops (rest (drop-while #(not (= (:id %) id)) log))))

(defn server-parented? [received-id]
  (some #(= received-id %) (map :id @history)))

(defn update-root-doc! [ops]
  (swap! root-document documents/apply-ops ops))

(defn append-to-history! [evt]
  (swap! history conj evt))

(defn write-message [msg]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json {:handlers transit-handlers/write-handlers})]
    (transit/write writer msg)
    (.toString out)))

(defn print-events [coll]
  (clojure.pprint/print-table [:id :parent-id :local-id :ops]
                              (map #(update-in % [:ops] operations/print-ops) coll)))

(defn rebase-incoming [{:keys [parent-id ops] :as data} new-base]
  (if (or (server-parented? parent-id)
          (not (seq new-base)))
    (let [data-with-id (assoc data :id (swap! doc-version inc)) ; dependency :(
          ops-since-id (operations-since-id parent-id new-base)]
      (if (seq ops-since-id)
        (let [server-ops (reduce composers/compose (map :ops ops-since-id))]
          (update-in data-with-id [:ops] #(first (transform % server-ops))))
        (do
          data-with-id)))
    (do
      (log/error "Rejected operation" parent-id  "Not parented on known history")
      (log/error new-base))))

(defn persist! [data]
  (update-root-doc! (:ops data))
  (append-to-history! data))

(defn handle-connections []
  (go-loop []
    (let [raw-data (<! input)
          in (ByteArrayInputStream. (.getBytes raw-data))
          reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
          data (transit/read reader)
          cleaned-data (rebase-incoming data @history)]
      (persist! cleaned-data)
      (broadcast (write-message cleaned-data))
      
      (print-events @history)
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
