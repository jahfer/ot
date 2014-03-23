(ns ot.crossover.transforms)

(defn op [type val]
  {:type type :val val})

(defn insert [operation]
  [operation, (op :ret 1)])

(defn retain [value]
  [(op :ret value) (op :ret value)])

(def mismatched-op-lengths (Exception. "Mismatched operation lengths"))

(defn conj-ops [op-lists new-ops]
  (map conj op-lists new-ops))

(defn assoc-op [new-ops ops]
  (conj-ops ops new-ops))

(defn update-head [ops val]
  (conj (rest ops) (-> (first ops)
                       (assoc :val val))))

(defn insert? [operation]
  (= :ins (:type operation)))

(defn retain? [operation]
  (= :ret (:type operation)))

(defn retain-both? [op1 op2]
  (= :ret (:type op1) (:type op2)))

(defn retain-ops [ops1 ops2]
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))]
    (cond
     (> val1 val2)
       (let [ops1 (update-head ops1 (- val1 val2))]
         [ops1 (rest ops2) (retain val2)])
     (= val1 val2)
       [(rest ops1) (rest ops2) (retain val2)]
     :else
       (let [ops2 (update-head ops2 (- val2 val1))]
         [(rest ops1) ops2 (retain val1)]))))

(defn start-index [ops]
  (if (retain? (first ops)) (:val (first ops)) 0))

(defn gen-inverse-ops [ops1 ops2 ops']
  (cond
   (insert? (first ops1))
     (let [ops' (-> (first ops1)
                    (insert)
                    (assoc-op ops'))]
       [(rest ops1) ops2 ops'])
   (insert? (first ops2))
     (let [ops' (-> (first ops2)
                    (insert)
                    (assoc-op (reverse ops'))
                    (reverse))]
       [ops1 (rest ops2) ops'])
   (retain-both? (first ops1) (first ops2))
     (let [[ops1 ops2 result] (retain-ops ops1 ops2)]
       [ops1 ops2 (assoc-op result ops')])
   :else
     (throw mismatched-op-lengths)))

(defn transform [a b]
  (loop [ops1 a, ops2 b, ops' [[] []]]
    (if (or (seq ops1) (seq ops2))
      (let [[ops1 ops2 ops'] (gen-inverse-ops ops1 ops2 ops')]
        (recur ops1 ops2 ops'))
      (map compress ops'))))

(defn gen-composed-ops [ops1 ops2 composed]
  (cond
   (insert? (first ops2))
     (let [result (conj composed (first ops2))]
       [ops1 (rest ops2) result])
   (retain-both? (first ops1) (first ops2))
     (let [[ops1 ops2 result] (retain-ops ops1 ops2)]
       [ops1 ops2 (conj composed (first result))])
   (and (insert? (first ops1)) (retain? (first ops2)))
     [(rest ops1) (rest ops2) (conj composed (first ops1))]
   :else
     (throw (Exception. "Operations are not composable"))))

(defn simplify [ops]
  (let [first-op (first ops)]
    (case (count ops)
      1 first-op
      2 (cond
          (retain? first-op) (second ops)
          (retain? (second ops)) first-op
          :else nil)
      3 (retain-both? first-op (nth ops 2)) (second ops)
      :else nil)))

(defn composable? [a b]
  (let [start-a (start-index a)
        start-b (start-index b)
        simple-a (simplify a)
        simple-b (simplify b)]
    (cond
     (not (or simple-a simple-b))
       false
     (and (insert? simple-a) (insert? simple-b))
       (= (+ start-a (count(:val simple-a))) start-b)
     :else
       false)))

(defn compose [a b]
  (if (composable? a b)
    (loop [ops1 a, ops2 b, composed []]
      (if (or (seq ops1) (seq ops2))
        (let [[ops1 ops2 composed] (gen-composed-ops ops1 ops2 composed)]
          (recur ops1 ops2 composed))
        composed))
  (throw (Exception. "Operations are not composable"))))

(defn merge-retains [& ops]
  (op :ret (reduce + (map :val ops))))

(defn compress [ops]
  (loop [ops ops, acc []]
    (if (empty? ops)
      acc
      (let [op1 (first ops), op2 (second ops)]
        (if (and (retain? op1) (retain? (peek acc)))
          (recur (next ops) (conj (pop acc) (merge-retains (peek acc) op1)))
          (recur (next ops) (conj acc op1)))))))
