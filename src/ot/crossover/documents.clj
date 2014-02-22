(ns ot.crossover.documents
  (:require [ot.crossover.transforms :as transforms]))

(defn apply-ins [trans doc]
  (str (:val trans) doc))

(defn apply-ret [trans doc]
  (let [n (:val trans)
        head (take n doc)
        tail (drop n doc)]
    [(apply str head) (apply str tail)]))

(defn apply-ops [doc ops]
  (let [operation (first ops)]
    (cond
     (transforms/insert? operation)
       (recur (apply-ins operation doc)
              (-> (rest ops)
                  (conj (transforms/op :ret 1))))
     (transforms/retain? operation)
       (let [[head tail] (apply-ret operation doc)]
         (str head (apply-ops tail (rest ops))))
     :else doc)))
