(ns ot.services.document-storage-service
  (:require [clojure.tools.logging :as log]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.services :refer [service-context]]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql]))

(defprotocol DocumentStorageService
  (insert [this table data])
  (select [this table conds]))

(tk/defservice cassandra-service
  DocumentStorageService
  [[:ConfigService get-in-config]]
  (init [this context]
        (log/debug "Initializing DocumentStorageService (Cassandra)")
        (let [cluster-nodes (get-in-config [:cassandra :cluster :addresses])
              keyspace (get-in-config [:cassandra :cluster :keyspace])]
          (assoc context :db (cc/connect cluster-nodes {:keyspace keyspace}))))
  (start [this context]
         (log/debug "Starting DocumentStorageService (Cassandra)")
         context)
  (stop [this context]
        (cc/disconnect (:db context))
        (dissoc context :db))
  (insert [this table data]
          (let [context (service-context this)]
            (cql/insert (:db context) table data)))
  (select [this table conds]
          (let [context (service-context this)]
            (apply cql/select (:db context) table conds))))
