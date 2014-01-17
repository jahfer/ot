(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]]
            [ajax.core :refer [GET POST]])
  (:import [goog.net Jsonp]
           [goog.net XhrIo]
           [goog Uri]))

(def live-url
  "/live")

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
                   (fn [e] (put! out e)))
    out))

(defn post [uri params]
  (let [out (chan)]
    (POST uri
          {:params params
           :error-handler (fn [res] (put! out res))})
    out))

(defn query-url [q]
  (str live-url))

(defn user-action []
  (.-value (dom/getElement "query")))

(defn render-actions [results]
  (str
     "<ul>"
     (apply str
        (for [result results]
          (str "<li>" result "</li>")))
   "</ul>"))

(defn init []
  (let [clicks (listen (dom/getElement "search") "click")
        results-view (dom/getElement "results")]
    (go (while true
          (<! clicks)
          (let [[_ results] (<! (post (query-url (user-action)) {:type :ret :val 1}))]
            (.log js/console (str "results: " results)))))))
            ;(set! (.-innerHTML results-view) (render-query results)))))))

(init)