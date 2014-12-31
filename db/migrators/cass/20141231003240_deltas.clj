(ns migrators.cass.20141231003240-deltas
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "deltas"
                      (cq/column-definitions {:document_id :uuid
                                              :id :int
                                              :parent_id :int
                                              :local_id :varchar
                                              :ops :varchar
                                              :primary-key [:document_id :id]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "deltas")))
