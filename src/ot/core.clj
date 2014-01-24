(ns ot.core
  (:require [ot.http.server :as server]))

(defn -main [& args]
  (server/start-server 3000))
