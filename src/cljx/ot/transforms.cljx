(ns ot.transforms
  (:require [ot.operations :as o]))

(defn insert [operation]
  [operation, (o/->Op :ret 1)])

(defn retain [value]
  [(o/->Op :ret value) (o/->Op :ret value)])

(defn retain-ops [ops1 ops2]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))]
    (cond
     (> val1 val2)
       (let [ops1 (update-in (vec ops1) [0 :val] #(- % val2))]
         [ops1 (rest ops2) (retain val2)])
     (= val1 val2)
       [(rest ops1) (rest ops2) (retain val2)]
     :else
       (let [ops2 (update-in (vec ops2) [0 :val] #(- % val1))]
         [(rest ops1) ops2 (retain val1)]))))

(defn compress [ops]
  (reduce (fn [acc op]
            (if (every? o/retain? (list op (peek acc)))
              (update-in acc [(-> acc count dec) :val] #(+ % (:val op)))
              (conj acc op)))
          []
          ops))

(defn gen-inverse-ops [ops1 ops2 ops']
  (cond
   (o/insert? (first ops1))
     (let [ops' (-> (first ops1)
                    (insert)
                    (o/assoc-op ops'))]
       [(rest ops1) ops2 ops'])
   (o/insert? (first ops2))
     (let [ops' (-> (first ops2)
                    (insert)
                    (o/assoc-op (reverse ops'))
                    (reverse))]
       [ops1 (rest ops2) ops'])
   (every? o/retain? (map first (list ops1 ops2)))
     (let [[ops1 ops2 result] (retain-ops ops1 ops2)]
       [ops1 ops2 (o/assoc-op result ops')])
   (every? o/delete? (map first (list ops1 ops2)))
     (let [val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> (- val1) (- val2)) [(update-in ops1 [0 :val] #(- % val2)) (rest ops2) ops']
        (= val1 val2) [(rest ops1) (rest ops2) ops']
        :else [(rest ops1) (update-in ops2 [0 :val] #(- % val1))]))
   (and (o/delete? (first ops1)) (o/retain? (first ops2)))
     (let [val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> val1 val2) [(update-in ops1 [0 :val] #(+ % val2)) (rest ops2) (o/assoc-op (first ops2) ops')]
        (= val1 val2) [(rest ops1) (rest ops2) [(conj (first ops') (first ops1)) (second ops')]]
        :else [(rest ops1) (+ val1 val2) (o/assoc-op (assoc (first ops1) :val (- val1)) ops')]))
   (and (o/retain? (first ops1)) (o/delete? (first ops2)))
     (let [val1 (:val (first ops1))
           val2 (:val (first ops2))]
       (cond
        (> val1 val2) [(update-in ops1 [0 :val] #(- % val2)) (rest ops2) (o/assoc-op (assoc (first ops2) :val (- val2)) ops')]
        (= val1 val2) [(rest ops1) (rest ops2) [(first ops') (conj (second ops') (first ops2))]]
        :else [(rest ops1) (update-in ops2 [0 :val] #(- % val1)) (o/assoc-op (first ops1) ops')]))
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
          (o/retain? first-op) (second ops)
          (o/retain? (second ops)) first-op)
      3 (when (every? o/retain? (list first-op (peek ops)))
          (second ops))
      :else nil)))
