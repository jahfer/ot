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

(defn async-handler [req]
  (with-channel req ch
    (println "New WebSocket channel:" ch)
    (on-close ch (fn [status] (println "channel closed: " status)))
    (on-receive ch (fn [data]
                     (put! input data)
                     (send! ch data)))))

(go
 (while true
   (let [data (<! input)
          parsed (clojure.edn/read-string data)]
         (println "type:" (:type parsed) "val:" (:val parsed)))))
