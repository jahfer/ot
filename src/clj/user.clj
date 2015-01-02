(ns user
  (:require [clojure.pprint :refer (pprint)]
            [ot.services.web-service :refer [web-service]]
            [ot.services.websocket-service :refer [websocket-service]]
            [ot.services.document-storage-service :refer [cassandra-service]]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.trapperkeeper.app :as tkapp]
            [clojure.tools.namespace.repl :refer (refresh)]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (fn [_] (tk/build-app
                           [web-service
                            websocket-service
                            cassandra-service]
                           {:global
                              {:logging-config "./resources/logback-dev.xml"}
                            :websocket {:port 3000}
                            :editor-web {:url-prefix "/editor"}
                            :cassandra {:cluster {:addresses ["127.0.0.1"]
                                                  :keyspace "ot_dev"}}})))
  (alter-var-root #'system tkapp/init)
  (tkapp/check-for-errors! system))

(defn start []
  (alter-var-root #'system
                  (fn [s] (if s (tkapp/start s))))
  (tkapp/check-for-errors! system))

(defn stop []
  (alter-var-root #'system
                  (fn [s] (when s (tkapp/stop s)))))

(defn go []
  (init)
  (start))

(defn context []
  @(tkapp/app-context system))

(defn print-context []
  (pprint (context)))

(defn reset []
  (stop)
  (refresh :after 'user/go))
