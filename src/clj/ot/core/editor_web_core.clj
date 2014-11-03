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
(def history (atom (sorted-set-by (fn [a b]
                                    (< (:id a) (:id b))))))
(def doc-version (atom 0))

(defn transaction-id [text]
  (digest/md5 text))

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
        (if (or (server-parented? parent-id) (not (seq @history)))
          (let [ops-since-id (operations-since-id parent-id)
                data (assoc data :id (swap! doc-version inc))]
            (log/info "ID assigned" data)
            (if (seq ops-since-id)
              (let [_ (log/info "Rebasing incoming operations")
                    _ (log/debug "Ops diff:" (map :ops ops-since-id))
                    server-ops (reduce composers/compose (map :ops ops-since-id))
                    _ (log/debug "Server ops:" server-ops)
                    evt (update-in data [:ops] #(first (transform % server-ops)))]
                (update-root-doc! (:ops evt))
                (append-to-history! evt)
                (log/info "Applied event" evt)
                (broadcast (write-message evt)))
              (do
                (log/info "Clean merge")
                (append-to-history! data)
                (update-root-doc! ops)
                (broadcast (write-message data)))))
          (do
            (log/error "Rejected operation" parent-id  "Not parented on server's history")
            (log/error @history)))))))

;; (defn handle-connections []
;;   (go
;;     (while true
;;       (let [data (<! input)
;;             in (ByteArrayInputStream. (.getBytes data))
;;             reader (transit/reader in :json {:handlers transit-handlers/read-handlers})
;;             parsed-data (transit/read reader)]

;;         (println)
;;         (println (->> (repeat "=") (take 120) clojure.string/join))

;;         (let [{:keys [ops parent-id] :as formatted-data} parsed-data]
;;           (println "RECEIVED OPERATION")
;;           (print-ops [formatted-data])

;;           (if (or (server-parented? parent-id) (not (seq @history)))
;;             (do
;;               (let [ops-since-id (operations-since-id parent-id)]
                
;;                 (println)
;;                 (println "OPERATIONS SINCE" parent-id)
;;                 (if (seq ops-since-id)
;;                   (print-ops ops-since-id)
;;                   (println "No operations found."))

;;                 (if (seq ops-since-id)
;;                   ; rebase incoming operations
;;                   (let [server-ops (reduce composers/compose (map :ops ops-since-id))
;;                         evt (update-in formatted-data [:ops] #(first (transform % server-ops)))
;;                         out (ByteArrayOutputStream. 4096)
;;                         writer (transit/writer out :json {:handlers transit-handlers/write-handlers})]

;;                     (update-root-doc! (:ops evt))
;;                     (append-to-history! evt)
                    
;;                     (transit/write writer evt)
;;                     (broadcast (.toString out)))
;;                   (do
;;                     (append-to-history! formatted-data)
;;                     (update-root-doc! ops)
;;                     (broadcast data))))
              
;;               (println)
;;               (println "HISTORY")
;;               (print-ops @history)
              
;;               (println)
;;               (println "RESULTING TEXT")
;;               (println @root-document))
;;             ; else
;;             (println "!!!" "REJECTED OPERATION")))))))

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
