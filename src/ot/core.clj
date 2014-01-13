(ns ot.core)

(require 'digest)

(defn doc-id [contents]
  (digest/md5 contents))

(defn op [type val]
  {:type type :val val})

(defn insert-trans [op1 ops1' ops2']
  [(conj ops1' op1) (conj ops2' (op :ret 1))])

(defn retain-trans [value ops1' ops2']
  (let [ret (op :ret value)]
    [(conj ops1' ret) (conj ops2' ret)]))

(defn ot [ops1 ops2 ops1' ops2']
  (if (or (seq ops1) (seq ops2))
    (let [op1 (first ops1)
          op2 (first ops2)
          type1 (:type op1)
          type2 (:type op2)]
      (cond
       (= :ins type1)
         (let [[ops1' ops2'] (insert-trans op1 ops1' ops2')]
           (recur (rest ops1) ops2 ops1' ops2'))
       (= :ins type2)
         (let [[ops2' ops1'] (insert-trans op2 ops2' ops1')]
           (recur ops1 (rest ops2) ops1' ops2'))

       (= :ret type1 type2)
         (let [val1 (:val op1)
               val2 (:val op2)]
           (cond
            (> val1 val2)
              (let [ops1 (conj (rest ops1) (assoc (first ops1) :val (- val1 val2)))
                    ops2 (rest ops2)
                    [ops1' ops2'] (retain-trans val2 ops1' ops2')]
                (recur ops1 ops2 ops1' ops2'))
            (= val1 val2)
              (let [ops1 (rest ops1)
                    ops2 (rest ops2)
                    [ops1' ops2'] (retain-trans val2 ops1' ops2')]
                (recur ops1 ops2 ops1' ops2'))
            :else
              (let [ops1 (rest ops1)
                    ops2 (conj (rest ops2) (assoc (first ops2) :val (- val2 val1)))
                    [ops1' ops2'] (retain-trans val1 ops1' ops2')]
                (recur ops1 ops2 ops1' ops2'))))))

    [ops1' ops2']))

(defn transform [ops1 ops2]
  (let [ops1' []
        ops2' []]
     (ot ops1 ops2 ops1' ops2')))