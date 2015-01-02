(ns migrators.cass.20141231003240-deltas
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "deltas"
                      (cq/column-definitions {:documentid :uuid
                                              :deltaid :int
                                              :operations :text
                                              :primary-key [:documentid :deltaid]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "deltas")))
