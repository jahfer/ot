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

(def root-document (atom "Hullo"))
(def history (atom #{}))

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

(defn indices [pred coll]
   (keep-indexed #(when (pred %2) %1) coll))

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(defn history-by-id []
  (clojure.set/index @history :id))

(defn operations-since-id [id]
  (let [n (first (indices #(= (:parent-id %) id) @history))]
    (when n
      (drop n @history))))

(defn server-parented? [received-id]
  (some #(= received-id %) (map :id @history)))

(defn print-ops [coll]
  (clojure.pprint/print-table [:id :parent-id :ops]
                              (mapv (fn [data]
                                      (update-in data [:ops] (fn [o]
                                                               (mapv (fn [{:keys [type val]}] {type val}) o))))
                                    coll)))

(defn update-root-doc! [ops]
  (swap! root-document documents/apply-ops ops))

(defn append-to-history! [evt]
  (swap! history conj evt))

(defn handle-connections []
  (go
    (while true
      (let [data (<! input)
            in (ByteArrayInputStream. (.getBytes data))
            reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
            parsed-data (transit/read reader)]

        (println)
        (println (->> (repeat "=") (take 120) clojure.string/join))

        (let [{:keys [id ops parent-id] :as formatted-data} parsed-data]
          (println "RECEIVED OPERATION")
          (print-ops [formatted-data])

          (if (or (server-parented? parent-id) (not (seq @history)))
            (do
              (let [ops-since-id (operations-since-id parent-id)]
                
                (println)
                (println "OPERATIONS SINCE" parent-id)
                (if (seq ops-since-id)
                  (print-ops ops-since-id)
                  (println "No operations found."))

                (if (seq ops-since-id)
                  (let [server-ops (reduce composers/compose (map :ops ops-since-id))
                        [ops' _] (transform ops server-ops)
                        evt (assoc-in formatted-data [:ops] ops')
                        out (ByteArrayOutputStream. 4096)
                        writer (transit/writer out :json {:handlers transit-handlers/write-handlers})]

                    (update-root-doc! ops')
                    
                    (let [evt (assoc-in evt [:parent-id] [(transaction-id @root-document)])]
                      (append-to-history! evt)
                      (transit/write writer evt)
                      (broadcast (.toString out))))
                  (do
                    (append-to-history! formatted-data)
                    (update-root-doc! ops)
                    (broadcast data))))
              
              (println)
              (println "HISTORY")
              (print-ops @history)
              
              (println)
              (println "RESULTING TEXT")
              (println @root-document))
            ; else
            (println "!!!" "REJECTED OPERATION")))))))

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
