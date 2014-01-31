(ns ot.crossover.documents
  (:require [ot.crossover.transforms :as transforms]))

(defn apply-ins [trans doc]
  (str (:val trans) doc))

(defn apply-ret [trans doc]
  (let [n (:val trans)
        head (take n doc)
        tail (drop n doc)]
    [(apply str head) (apply str tail)]))

(defn apply-ops [ops doc]
  (let [operation (first ops)]
    (cond
     (transforms/insert? operation)
       (recur (-> (rest ops)
                  (conj (transforms/op :ret 1))) (apply-ins operation doc))
     (transforms/retain? operation)
       (let [[head tail] (apply-ret operation doc)]
         (str head (apply-ops (rest ops) tail)))
     :else doc)))
