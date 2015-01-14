(ns ot.core.document-core
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer :all]
            [ot.transforms :as transforms]
            [ot.documents :as documents]
            [ot.composers :as composers]
            [ot.operations :as operations]))

(def root-document (atom "Hullo"))
(def deltaids (atom {}))
(def history (atom (sorted-set-by (fn [a b]
                                    (< (:id a) (:id b))))))

(defn- tag-operation [data]
  (let [documentid #uuid "70ef8740-9237-11e4-aec4-054abea3cfa4"]
    (log/debug @deltaids)
    (swap! deltaids update-in [documentid] inc')
    (assoc data :id (get @deltaids documentid))))

(defn- server-parented? [received-id]
  (some #(= received-id %) (map :id @history)))

(defn- operations-since-id [id log]
  (map :ops (rest (drop-while #(not (= (:id %) id)) log))))

(defn- update-root-doc! [ops]
  (swap! root-document documents/apply-ops ops))

(defn- append-to-history! [evt]
  (swap! history conj evt))

(defn- persist! [document-store {:keys [id ops] :as data}]
  ((:insert document-store) "deltas"
   {:documentid #uuid "70ef8740-9237-11e4-aec4-054abea3cfa4"
    :deltaid id
    :operations (pr-str ops)})
  (update-root-doc! ops)
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

(defn submit-request [document-store opdata]
  (let [cleaned-data (-> opdata
                         (rebase-incoming @history)
                         (tag-operation))]
    (log/info "Assigned ID:" (:id cleaned-data))
    (persist! document-store cleaned-data)
    (print-events @history)
    cleaned-data))

(defn request-document [document-store documentid]
  (let [select-fn (:select document-store)
        deltas (select-fn "deltas"
                          [(columns :operations :deltaid)
                           (where [[= :documentid documentid]])
                           (order-by [:deltaid :asc])])]
    (if (empty? deltas)
      (when-not (get @deltaids documentid)
        (swap! deltaids assoc documentid 0N))
      (let [hash-deltas (map #(clojure.edn/read-string
                               {:readers {'ot.operations.Op ot.operations/map->Op}}
                               (:operations %))
                             deltas)
            _ (log/debug (pr-str hash-deltas))
            composed-ops (reduce composers/compose hash-deltas)
            deltaid (:deltaid (peek deltas))]
        (when-not (get @deltaids documentid)
          (swap! deltaids assoc documentid deltaid))
        [:text (documents/apply-ops "" composed-ops)
         :deltaid deltaid]))))
