(ns ot.composers
  #+clj (:use[clojure.core.match :only (match)])
  (:require [ot.operations :as o]
            [ot.transforms :as t]
            #+cljs [cljs.core.match])
  #+cljs (:require-macros [cljs.core.match.macros :refer [match]]))

(defmulti compose-ops 
  (fn [ops1 ops2 _]
    (let [op1 (first ops1)
          op2 (first ops2)]
      (match [(:type op1) (:type op2)]
             [:del _]    :apply-first
             [_ :ins]    :apply-second
             [:ret :ret] :retain
             [:ins :ret] :insert-and-retain
             [:ins :del] :insert-and-delete
             [:ret :del] :retain-and-delete))))

(defmethod compose-ops :retain [ops1 ops2 out]
  (let [[ops1 ops2 [result]] (t/retain-ops ops1 ops2)]
    [ops1 ops2 (conj out result)]))

(defmethod compose-ops :apply-first [ops1 ops2 out]
  [(rest ops1) ops2 (->> ops1 first (conj out))])

(defmethod compose-ops :apply-second [ops1 ops2 out]
  [ops1 (rest ops2) (->> ops2 first (conj out))])

(defmethod compose-ops :insert-and-retain [ops1 ops2 out]
  (let [ops2 (if (= 1 (get-in ops2 [0 :val]))
               (rest ops2)
               (update-in ops2 [0 :val] dec))]
       [(rest ops1) ops2 (conj out (first ops1))]))

(defmethod compose-ops :insert-and-delete [ops1 ops2 out]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))
        ops1' (if (> val1 val2) (assoc-in ops1 [0 :val] (- val1 val2)) (rest ops1))
        ops2' (if (< val1 val2) (assoc-in ops2 [0 :val] (+ val1 val2)) (rest ops2))]
    [ops1' ops2' out]))

(defmethod compose-ops :retain-and-delete [ops1 ops2 out]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))
        ops1' (if (> val1 val2) (assoc-in ops1 [0 :val] (+ val1 val2)) (rest ops1))
        ops2' (if (< val1 val2) (assoc-in ops2 [0 :val] (+ val2 val1)) (rest ops2))]
    [ops1' ops2' out]))

(defn compose [a b]
  (loop [ops1 a, ops2 b, composed []]
    (if (or (seq ops1) (seq ops2))
      (let [[ops1 ops2 composed] (compose-ops ops1 ops2 composed)]
        (recur ops1 ops2 composed))
      composed)))
