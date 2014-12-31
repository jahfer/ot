(ns migrators.cass.20141231004054-documents
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "documents"
                      (cq/column-definitions {:id :uuid
                                              :owner_id :uuid
                                              :body :text
                                              :last_delta :int
                                              :created_at :timestamp
                                              :primary-key [:id]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "documents")))
