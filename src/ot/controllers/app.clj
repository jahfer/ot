(ns ot.controllers.app
  (:use [org.httpkit.server]))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn live-handler [type val]
  (println (str "type: " type " val: " val))
  (generate-response {:hello type}))

(defn async-handler [req]
  (with-channel req ch
    (println "New WebSocket channel:" ch)
    (on-close ch (fn [status] (println "channel closed: " status)))
    (on-receive ch (fn [data]
                     (send! ch data)))))
