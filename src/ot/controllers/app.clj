(ns ot.controllers.app
  (:use [org.httpkit.server])
  (:require [clojure.core.async :refer [go put! <! chan]]
            [clojure.tools.logging :as log]
            [ot.crossover.documents :refer :all]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn live-handler [type val]
  (log/info (str "type: " type " val: " val))
  (generate-response {:hello type}))

(def input (chan))
(def clients (atom {}))
(def document (atom ""))

(defn broadcast [msg]
    (log/debug "emitting message to client client" msg)
  (doseq [client @clients]
    (send! (key client) msg)))

(defn handle-incoming []
  (go
   (while true
     (let [data (<! input)
           parsed (clojure.edn/read-string data)
           ops (:ops parsed)]
       (log/info "Received from client:" data)
       (swap! document apply-ops ops)
       (log/debug "applied document state:" @document)
       (broadcast data)))))

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (log/info "New WebSocket channel:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (println "channel closed:" status)))))

(handle-incoming)
