(ns ot.transforms
  (:require [ot.operations :as operations]))

(defn delete-both? [op1 op2]
  (= :del (:type op1) (:type op2)))

(defn retain-both? [op1 op2]
  (= :ret (:type op1) (:type op2)))

(defn insert [operation]
  [operation, (operations/->Op :ret 1)])

(defn retain [value]
  [(operations/->Op :ret value) (operations/->Op :ret value)])

(defn retain-ops [ops1 ops2]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))]
    (cond
     (> val1 val2)
       (let [ops1 (operations/update-head ops1 (- val1 val2))]
         [ops1 (rest ops2) (retain val2)])
     (= val1 val2)
       [(rest ops1) (rest ops2) (retain val2)]
     :else
       (let [ops2 (operations/update-head ops2 (- val2 val1))]
         [(rest ops1) ops2 (retain val1)]))))

(defn start-index [ops]
  (if (operations/retain? (first ops)) (:val (first ops)) 0))

(defn merge-retains [& ops]
  (operations/->Op :ret (reduce + (map :val ops))))

(defn compress [ops]
  (loop [ops ops, acc []]
    (if (empty? ops)
      acc
      (let [op1 (first ops), op2 (second ops)]
        (if (and (operations/retain? op1) (operations/retain? (peek acc)))
          (recur (next ops) (conj (pop acc) (merge-retains (peek acc) op1)))
          (recur (next ops) (conj acc op1)))))))

(defn gen-inverse-ops [ops1 ops2 ops']
  (cond
   (operations/insert? (first ops1))
     (let [_ (println "-- insert first")
           ops' (-> (first ops1)
                    (insert)
                    (operations/assoc-op ops'))]
       [(rest ops1) ops2 ops'])
   (operations/insert? (first ops2))
     (let [_ (println "-- insert second")
           ops' (-> (first ops2)
                    (insert)
                    (operations/assoc-op (reverse ops'))
                    (reverse))]
       [ops1 (rest ops2) ops'])
   (retain-both? (first ops1) (first ops2))
     (let [_ (println "-- retain both")
           [ops1 ops2 result] (retain-ops ops1 ops2)]
       [ops1 ops2 (operations/assoc-op result ops')])
   (delete-both? (first ops1) (first ops2))
     (let [_ (println "-- delete both")
           val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> (- val1) (- val2)) [(operations/update-head ops1 (- val1 val2)) (rest ops2) ops']
        (= val1 val2) [(rest ops1) (rest ops2) ops']
        :else [(rest ops1) (operations/update-head ops2 (- val2 val1))]))
   (and (operations/delete? (first ops1)) (operations/retain? (first ops2)))
     (let [_ (println "-- delete + retain")
           val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> val1 val2) [(operations/update-head ops1 (+ val1 val2)) (rest ops2) (operations/assoc-op (first ops2) ops')]
        (= val1 val2) [(rest ops1) (rest ops2) [(conj (first ops') (first ops1)) (second ops')]]
        :else [(rest ops1) (+ val1 val2) (operations/assoc-op (assoc (first ops1) :val (- val1)) ops')]))
   (and (operations/retain? (first ops1)) (operations/delete? (first ops2)))
     (let [_ (println "-- retain + delete")
           val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> val1 val2) [(operations/update-head ops1 (- val1 val2)) (rest ops2) (operations/assoc-op (assoc (first ops2) :val (- val2)) ops')]
        (= val1 val2) [(rest ops1) (rest ops2) [(first ops') (conj (second ops') (first ops2))]]
        :else [(rest ops1) (operations/update-head ops2 (- val2 val1)) (operations/assoc-op (first ops1) ops')]))
   :else
     (do
       (println "no cond found")
       nil)))

(defn transform [a b]
  (loop [ops1 a, ops2 b, ops' [[] []]]
    (if (or (seq ops1) (seq ops2))
      (let [[ops1 ops2 ops'] (gen-inverse-ops ops1 ops2 ops')]
        (recur ops1 ops2 ops'))
      (map compress ops'))))

(defn simplify [ops]
  (let [first-op (first ops)]
    (case (count ops)
      1 first-op
      2 (cond
          (operations/retain? first-op) (second ops)
          (operations/retain? (second ops)) first-op
          :else nil)
      3 (if (retain-both? first-op (peek ops)) (second ops) nil)
      :else nil)))
