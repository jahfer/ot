(ns ot.documents
  (:require [ot.transforms :as transforms]
            [clojure.string :as str]))

(defn apply-ins [trans doc]
  (str (:val trans) doc))

(defn apply-ret [trans doc]
  (map str/join (split-at (:val trans) doc)))

(defn apply-ops [doc ops]
  (let [operation (first ops)]
    (cond
     (transforms/insert? operation)
       (recur (apply-ins operation doc)
              (-> (rest ops)
                  (conj (transforms/->Op :ret 1))))
     (transforms/retain? operation)
       (let [[head tail] (apply-ret operation doc)]
         (str head (apply-ops tail (rest ops))))
     :else doc)))
