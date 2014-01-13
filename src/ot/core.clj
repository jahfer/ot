(ns ot.core)

(require 'digest)

(defn doc-id [contents]
  (digest/md5 contents))

(defn op [type val]
  {:type type :val val})

(defn assoc-ops [op-lists new-ops]
  (map conj op-lists new-ops))

(defn insert [operation]
  [operation, (op :ret 1)])

(defn retain [value]
    [(op :ret value) (op :ret value)])

(defn assoc-op [trans ops value]
  (assoc-ops ops (trans value)))

(defn update-head [ops val]
  (conj (rest ops) (assoc (first ops) :val val)))

(defn ot [ops1 ops2 ops1' ops2']
  (if (or (seq ops1) (seq ops2))
    (let [op1 (first ops1)
          op2 (first ops2)
          type1 (:type op1)
          type2 (:type op2)]
      (cond
       (= :ins type1)
         (let [[ops1' ops2'] (assoc-op insert [ops1' ops2'] op1)]
           (recur (rest ops1) ops2 ops1' ops2'))
       (= :ins type2)
         (let [[ops2' ops1'] (assoc-op insert [ops2' ops1'] op2)]
           (recur ops1 (rest ops2) ops1' ops2'))

       (= :ret type1 type2)
         (let [val1 (:val op1)
               val2 (:val op2)]
           (cond
            (> val1 val2)
              (let [ops1 (update-head ops1 (- val1 val2))
                    [ops1' ops2'] (assoc-op retain [ops1' ops2'] val2)]
                (recur ops1 (rest ops2) ops1' ops2'))
            (= val1 val2)
              (let [[ops1' ops2'] (assoc-op retain [ops1' ops2'] val2)]
                (recur (rest ops1) (rest ops2) ops1' ops2'))
            :else
              (let [ops2 (update-head ops2 (- val2 val1))
                    [ops1' ops2'] (assoc-op retain [ops1' ops2'] val1)]
                (recur (rest ops1) ops2 ops1' ops2'))))))

    [ops1' ops2']))

(defn transform [ops1 ops2]
  (ot ops1 ops2 [] []))