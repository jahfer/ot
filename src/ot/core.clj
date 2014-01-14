(ns ot.core)

(require 'digest)

(defn doc-id [contents]
  (digest/md5 contents))

(defn op [type val]
  {:type type :val val})

(defn insert [operation]
  [operation, (op :ret 1)])

(defn retain [value]
  [(op :ret value) (op :ret value)])

(defn conj-ops [op-lists new-ops]
  (map conj op-lists new-ops))

(defn assoc-op [new-ops ops]
  (conj-ops ops new-ops))

(defn update-head [ops val]
  (conj (rest ops) (-> (first ops)
                       (assoc :val val))))

(defn insert? [ops]
  (-> (first ops)
      (:type)
      (= :ins)))

(defn retain-both? [ops1 ops2]
  (= :ret (:type (first ops1)) (:type (first ops2))))

(defn retain-ops [ops1 ops2 ops']
  (let [val1 (:val (first ops1))
        val2 (:val (first ops2))]

    (cond

     (> val1 val2)
       (let [ops1 (update-head ops1 (- val1 val2))
             ops' (-> (retain val2)
                      (assoc-op ops'))]
         [ops1 (rest ops2) ops'])

     (= val1 val2)
       (let [ops' (-> (retain val2)
                      (assoc-op ops'))]
         [(rest ops1) (rest ops2) ops'])

     :else
       (let [ops2 (update-head ops2 (- val2 val1))
             ops' (-> (retain val1)
                      (assoc-op ops'))]
         [(rest ops1) ops2 ops']))))

(defn gen-inverse-ops [ops1 ops2 ops']
  (cond
   (insert? ops1)
     (let [ops' (-> (first ops1)
                    (insert)
                    (assoc-op ops'))]
       [(rest ops1) ops2 ops'])

   (insert? ops2)
     (let [ops' (-> (first ops2)
                    (insert)
                    (assoc-op (reverse ops'))
                    (reverse))]
       [ops1 (rest ops2) ops'])

   (retain-both? ops1 ops2)
     (retain-ops ops1 ops2 ops')))

(defn ot [ops1 ops2 ops']
  (if (or (seq ops1) (seq ops2))
    (let [[ops1 ops2 ops'] (gen-inverse-ops ops1 ops2 ops')]
      (recur ops1 ops2 ops'))
    ops'))

(defn transform [ops1 ops2]
  (ot ops1 ops2 [[] []]))