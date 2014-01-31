(ns ot.controllers.app
  (:use [org.httpkit.server])
  (:require [clojure.core.async :refer [go put! <! chan]]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn live-handler [type val]
  (println (str "type: " type " val: " val))
  (generate-response {:hello type}))

(def input (chan))
(def clients (atom {}))

(defn async-handler [req]
  (with-channel req ch
    (swap! clients assoc ch true)
    (println "New WebSocket channel:" ch)
    (on-receive ch (fn [data]
                     (put! input data)))
    (on-close ch (fn [status]
                   (swap! clients dissoc ch)
                   (println "channel closed: " status)))))

(go
 (while true
   (let [data (<! input)
         parsed (clojure.edn/read-string data)]
         (println "id:" (:id parsed)
                  "parent-id:" (:parent-id parsed)
                  "type:" (:type (:op parsed))
                  "val:" (:val (:op parsed)))
     (doseq [client @clients]
       (send! (key client) data)))))
