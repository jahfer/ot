(ns ot.services.document-service
  (:require [clojure.tools.logging :as log]
            [ot.core.document-core :as core]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.services :refer [service-context]]
            [puppetlabs.trapperkeeper.app :refer [get-service]]))

(defprotocol DocumentService
  (request-history   [this documentid])
  (request-document  [this documentid])
  (request-documents [this])
  (submit-request    [this data]))

(tk/defservice document-service
  DocumentService
  [[:ConfigService get-in-config]
   DocumentStorageService]
  (init [this context]
        (log/debug "Initializing DocumentService")
        context)
  (start [this context]
         (log/debug "Starting DocumentService")
         context)
  (stop [this context] context)
  (request-documents [this]
                     (core/request-documents DocumentStorageService))
  (request-history  [this documentid])
  (request-document [this documentid]
                    (core/request-document DocumentStorageService documentid))
  (submit-request  [this data]
                   (core/submit-request DocumentStorageService data)))
