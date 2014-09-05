(ns ot.documents
  (:require [ot.operations :as operations]
            [ot.transforms :as transforms]
            [clojure.string :as str]))

(defn apply-ins [trans doc]
  (str (:val trans) doc))

(defn apply-ret [trans doc]
  (map str/join (split-at (:val trans) doc)))

(defn apply-ops [doc ops]
  (let [operation (first ops)]
    (cond
     (operations/insert? operation)
       (recur (apply-ins operation doc)
              (-> (rest ops)
                  (conj (operations/->Op :ret 1))))
     (operations/retain? operation)
       (let [[head tail] (apply-ret operation doc)]
         (str head (apply-ops tail (rest ops))))
     :else doc)))
