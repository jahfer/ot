(ns ot.controllers.documents
  (:use org.httpkit.server)
  (:require [clojure.core.async :refer [go put! <! chan]]
            [clojure.tools.logging :as log]
            [ot.crossover.documents :refer :all]
            [ot.crossover.transforms :refer :all]))

(def input (chan))
(def clients (atom {}))
(def document (atom ""))

(defn broadcast [msg]
  (log/debug "emitting message to client" msg)
  (Thread/sleep 2000)
  (doseq [client @clients]
    (send! (key client) msg)))

(defn handle-incoming []
  (go
    (while true
      (let [data (<! input)
            parsed-data (clojure.edn/read-string
                         {:readers {'ot.crossover.transforms.Op ot.crossover.transforms/map->Op}}
                         data)]
        (log/info "Received from client:" data)
        (broadcast data)))))

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New connection:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (log/info "closed channel:" status)))))

(handle-incoming)
