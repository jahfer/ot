(ns ot.core)

(require 'digest)

(defn doc-id [contents]
  (digest/md5 contents))

(defn op [type val]
  {:type type :val val})

(defn ot [ops1 ops2 ops1' ops2']
  (if (or (seq ops1) (seq ops2))
    (let [op1 (first ops1)
          op2 (first ops2)
          type1 (:type op1)
          type2 (:type op2)]
      (cond
       (= :ins type1)
         (let [ops1 (rest ops1)
               ops1' (conj ops1' op1)
               ops2' (conj ops2' (op :ret 1))]
           (recur ops1 ops2 ops1' ops2'))
       (= :ins type2)
         (recur ops1 (rest ops2) (conj ops1' (op :ret 1)) (conj ops2' op2))

       (= :ret type1 type2)
         (let [v1 (:val op1)
               v2 (:val op2)]
           (cond
            (> v1 v2)
              (let [ops1 (conj (rest ops1) (assoc-in (first ops1) [:val] (- v1 v2)))
                    ops2 (rest ops2)
                    ret (op :ret v2)]
                (recur ops1 ops2 (conj ops1' ret) (conj ops2' ret)))
            (= v1 v2)
              (let [ops1 (rest ops1)
                    ops2 (rest ops2)
                    ret (op :ret v2)]
                (recur ops1 ops2 (conj ops1' ret) (conj ops2' ret)))
            :else
              (let [ops1 (rest ops1)
                    ops2 (conj (rest ops2) (assoc-in (first ops2) [:val] (- v2 v1)))
                    ret (op :ret v1)]
                (recur ops1 ops2 (conj ops1' ret) (conj ops2' ret)))))))

    [ops1' ops2']))

(defn transform [ops1 ops2]
  (let [ops1' []
        ops2' []]
     (ot ops1 ops2 ops1' ops2')))