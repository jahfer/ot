(ns migrators.cass.20141231004111-users
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "users"
                      (cq/column-definitions {:id :uuid
                                              :name :varchar
                                              :email :varchar
                                              :password :varchar
                                              :created_at :timestamp
                                              :primary-key [:id]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "users")))
