(ns ot.core.editor-web-core
  (:use [compojure.core :only [defroutes GET]]
        org.httpkit.server)
  (:import [java.io ByteArrayInputStream]
           [java.io ByteArrayOutputStream])
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [compojure.route :as route]
            [cognitect.transit :as transit]
            [digest]
            [ot.templating.views :as views]
            [ot.transforms :refer :all]
            [ot.documents :as documents]
            [ot.composers :as composers]
            [ot.transit-handlers :as transit-handlers]
            [clojure.core.async :refer [go put! <! chan]]))

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
                              :body (pr-str {:id    (:id params)
                                             :doc   @root-document
                                             :tx-id @doc-version})}))
  (GET "/ws" [] async-handler)
  (GET "/iframe" [] (views/iframed-test)))

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(defn operations-since-id [id]
  (rest (drop-while #(not (= (:id %) id)) @history)))

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

(defn handle-connections []
  (go
    (while true
      (let [raw-data (<! input)
            in (ByteArrayInputStream. (.getBytes raw-data))
            reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
            {:keys [parent-id ops] :as data} (transit/read reader)]
        (println)
        (println (clojure.string/join (repeatedly 120 (fn [] "="))))
        (log/info "Received:")
        (clojure.pprint/print-table [:parent-id :local-id :ops] [data])
        (println)
        (if (or (server-parented? parent-id) (not (seq @history)))
          (let [ops-since-id (operations-since-id parent-id)
                data (assoc data :id (swap! doc-version inc))]
            (log/debug "ID assigned" (:id data))
            (if (seq ops-since-id)
              (let [_ (log/debug "Rebasing incoming operations")
                    server-ops (reduce composers/compose (map :ops ops-since-id))
                    _ (log/debug "Rebasing on tx IDs" (map :id ops-since-id))
                    evt (update-in data [:ops] #(first (transform % server-ops)))]
                (update-root-doc! (:ops evt))
                (append-to-history! evt)
                (broadcast (write-message evt))
                (log/info "Applied, stored and broadcasted:")
                (clojure.pprint/print-table [:id :parent-id :local-id :ops] [evt]))
              (do
                (log/debug "Clean merge")
                (append-to-history! data)
                (update-root-doc! ops)
                (broadcast (write-message data))
                (log/info "Applied, stored and broadcasted:")
                (clojure.pprint/print-table [:id :parent-id :local-id :ops] [data]))))
          (do
            (log/error "Rejected operation" parent-id  "Not parented on server's history")
            (log/error @history)))
        (println)
        (log/info "Current history state:")
        (clojure.pprint/print-table [:id :parent-id :ops] @history)))))

(defn shutdown []
  (doseq [client @clients]
    (close (key client))
    (swap! clients dissoc client))
  (log/info "Finished closing of websocket clients"))

(defn broadcast [msg]
  (Thread/sleep 2000)
  (doseq [client @clients]
    (send! (key client) msg)))
