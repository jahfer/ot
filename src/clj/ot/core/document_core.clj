(ns ot.core.document-core
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [ot.transforms :as transforms]
            [ot.documents :as documents]
            [ot.composers :as composers]
            [ot.operations :as operations]))

(def root-document (atom "Hullo"))
(def doc-version (atom 0))
(def history (atom (sorted-set-by (fn [a b]
                                    (< (:id a) (:id b))))))

(defn- tag-operation [data]
  (assoc data :id (swap! doc-version inc)))

(defn- server-parented? [received-id]
  (some #(= received-id %) (map :id @history)))

(defn- operations-since-id [id log]
  (map :ops (rest (drop-while #(not (= (:id %) id)) log))))

(defn- update-root-doc! [ops]
  (swap! root-document documents/apply-ops ops))

(defn- save-to-db! [{:keys [id ops]}]
  (let [conn (cc/connect ["localhost"])]
    (cql/use-keyspace conn "ot_dev")
    (cql/insert conn "deltas" {:documentid #uuid "70ef8740-9237-11e4-aec4-054abea3cfa4"
                               :deltaid id
                               :operations (pr-str ops)})))

(defn- append-to-history! [evt]
  (save-to-db! evt)
  (swap! history conj evt))

(defn- persist! [data]
  (update-root-doc! (:ops data))
  (append-to-history! data))

(defn- rebase-incoming [{:keys [parent-id ops] :as data} new-base]
  (if (or (server-parented? parent-id)
          (not (seq new-base)))
    (let [ops-since-id (operations-since-id parent-id new-base)]
      (if (seq ops-since-id)
        (let [server-ops (reduce composers/compose ops-since-id)]
          (update-in data [:ops] #(first (transforms/transform % server-ops))))
        data))
    (do
      (log/error "Rejected operation" parent-id  "Not parented on known history")
      (log/error new-base))))

(defn- print-events [coll]
  (clojure.pprint/print-table [:id :parent-id :local-id :ops]
                              (map #(update-in % [:ops] operations/print-ops) coll)))

(defn submit-request [opdata]
  (let [cleaned-data (-> opdata
                         (rebase-incoming @history)
                         (tag-operation))]
    (persist! cleaned-data)
    (print-events @history)
    cleaned-data))
