(ns ot.lib.util
  (:require [cljs.core.async :refer [put! chan]])
  (:use [jayq.core :only [$ on]]))

(defn listen [el type]
  (let [out (chan)]
    (on ($ el) type
        (fn [e] (put! out e)))
    out))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn keyFromCode [code]
  (.fromCharCode js/String code))

(defn toInt [num]
  (js/parseInt num 10))
