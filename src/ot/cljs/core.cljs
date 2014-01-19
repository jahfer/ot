(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [jayq.util :as jq-util])
  (:use-macros [dommy.macros :only [node sel sel1]])
  (:use [jayq.core :only [$ css html ajax on]]))

(defn listen [el type]
  (let [out (chan)]
    (on ($ el) type
        (fn [e] (put! out e)))
    out))

(defn post [uri params]
  (let [out (chan)]
    (ajax uri
          {:type "POST"
           :data params
           :contentType "application/edn"
           :success (fn [res] (put! out res))})
    out))

(defn init []
  (let [keypress (listen (sel1 :#editor) "keypress")]
    (go (while true
          (let [e (<! keypress)
                key (.-key e)]
            (jq-util/log (.-key e))
            (let [results (<! (post "/live" {:type :ins :val key}))]
              (jq-util/log (pr-str results))))))))

(init)