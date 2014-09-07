(ns ot.composers
  (:require [ot.operations :as o]
            [ot.transforms :as t]))

(defn composition-type [ops1 ops2 out]
  (let [op1 (first ops1)
        op2 (first ops2)]
    (cond
     (o/delete? op1)               :apply-first
     (o/insert? op2)               :apply-second
     (every? o/retain? [op1 op2])  :retain
     (o/insert? op1)
       (cond
        (o/delete? op2)            :insert-and-delete
        (o/retain? op2)            :insert-and-retain
        :else                      :apply-first)
     (o/delete? op2)               :apply-second)))

(defmulti compose-ops composition-type)

(defmethod compose-ops :retain [ops1 ops2 out]
  (let [[ops1 ops2 [result]] (t/retain-ops ops1 ops2)]
    [ops1 ops2 (conj out result)]))

(defmethod compose-ops :apply-first [ops1 ops2 out]
  [(rest ops1) ops2 (->> ops1 first (conj out))])

(defmethod compose-ops :apply-second [ops1 ops2 out]
  [ops1 (rest ops2) (->> ops2 first (conj out))])

(defmethod compose-ops :insert-and-delete [ops1 ops2 out]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))
        ops1' (if (> val1 val2) (assoc-in ops1 [0 :val] (- val1 val2)) (rest ops1))
        ops2' (if (< val1 val2) (assoc-in ops2 [0 :val] (+ val1 val2)) (rest ops2))]
    [ops1' ops2' out]))

(defmethod compose-ops :insert-and-retain [ops1 ops2 out]
  (let [ops2 (if (= 1 (get-in ops2 [0 :val]))
               (rest ops2)
               (update-in ops2 [0 :val] dec))]
       [(rest ops1) ops2 (conj out (first ops1))]))

(defn compose [a b]
  (loop [ops1 a, ops2 b, composed []]
    (if (or (seq ops1) (seq ops2))
      (let [[ops1 ops2 composed] (compose-ops ops1 ops2 composed)]
        (recur ops1 ops2 composed))
      composed)))
