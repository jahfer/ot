(ns ot.cljs.util.misc
  (:require [cljs.core.async :refer [put! chan]])
  (:use [jayq.core :only [$ on]]))

(defn listen [el type]
  (let [out (chan)]
    (on ($ el) type
        (fn [e] (put! out e)))
    out))
