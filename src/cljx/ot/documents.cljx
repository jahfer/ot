(ns ot.documents
  #+clj (:use[clojure.core.match :only (match)])
  (:require [ot.operations :as o]
            [ot.transforms :as transforms]
            [clojure.string :as str]
            #+cljs [cljs.core.match])
  #+cljs (:require-macros [cljs.core.match.macros :refer [match]]))

(defn apply-ins [{c :val} doc]
  (str c doc))

(defn apply-ret [{n :val} doc]
  (let [[head tail] (map str/join (split-at n doc))]
    {:head head :tail tail}))

(defn apply-del [{n :val} doc]
  (str/join (drop n doc)))

(defn exec-ops [doc ops]
  (let [op (first ops)]
    (match (:type op)
           :ins  {:tail (apply-ins op doc)
                  :ops (conj (rest ops) (o/->Op :ret 1))}
           :del  {:tail (apply-del op doc)
                  :ops (rest ops)}
           :ret  (merge (apply-ret op doc) {:ops (rest ops)})
           :else {:tail doc
                  :ops nil})))

(defn apply-ops [document oplist]
  (loop [last-head "", doc document, ops oplist]
    (let [{:keys [head tail ops]} (exec-ops doc ops)
          last-head (if (nil? head)
                      last-head
                      (str last-head head))]
      (if (seq? ops)
        (recur last-head tail ops)
        last-head))))
