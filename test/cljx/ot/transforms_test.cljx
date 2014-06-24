(ns ot.transforms-test
  (:require [clojure.test :refer :all]
            [ot.transforms :refer :all]))

(def document "go")

(def op-tom
  [(->Op :ret 2) (->Op :ins "a")])
(def op-jerry
  [(->Op :ret 2) (->Op :ins "t")])

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(deftest insert-test
  (testing "An insert operation will produce the correct resulting pair"
    (let [[op1 op2] (insert (->Op :ins "a"))]
      (is (= op1 (->Op :ins "a")))
      (is (= op2 (->Op :ret 1))))))

(deftest retain-test
  (testing "retain-trans will return a vec of two retain operations of the same value"
    (let [[a b] (retain 5)]
      (is (= a (->Op :ret 5)))
      (is (= b (->Op :ret 5))))))

(deftest update-head-test
  (testing "update-head will change the :val of the head of the vec passed"
    (not (= (:val (first op-tom)) 4))
    (let [new-list (update-head op-tom 4)]
      (is (= (:val (first new-list)) 4)))))

(deftest transform-test
  (testing "Transforming two operations results in the correct pair of functions as response"
    (let [[a' b'] (transform op-tom op-jerry)
          expect-ops1' [(->Op :ret 2) (->Op :ins "a") (->Op :ret 1)]
          expect-ops2' [(->Op :ret 3) (->Op :ins "t")]]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2')))))

(deftest compress-test
  (testing "#compress will join all neighboring like items"
    (let [ops1 [(->Op :ret 2) (->Op :ret 1) (->Op :ins "a") (->Op :ret 1) (->Op :ret 3)]
          ops2 [(->Op :ret 1) (->Op :ret 1) (->Op :ret 1) (->Op :ret 1) (->Op :ins "b")]
          result1 (compress ops1)
          result2 (compress ops2)]
      (is (= result1 [(->Op :ret 3) (->Op :ins "a") (->Op :ret 4)]))
      (is (= result2 [(->Op :ret 4) (->Op :ins "b")])))))

(deftest compose-test
  (testing "Composing two lists of operations results in a single list of all combined operations"
    (let [comp (compose [(->Op :ret 1) (->Op :ins "o")] [(->Op :ret 2) (->Op :ins "t")])]
      (is (= comp [(->Op :ret 1) (->Op :ins "o") (->Op :ins "t")])))))
