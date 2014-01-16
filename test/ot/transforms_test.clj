(ns ot.transforms-test
  (:require [clojure.test :refer :all]
            [ot.transforms :refer :all]))

(def document "go")

(def op-tom
  [(op :ret 2) (op :ins "a")])
(def op-jerry
  [(op :ret 2) (op :ins "t")])

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(deftest insert-test
  (testing "An insert operation will produce the correct resulting pair"
    (let [[op1 op2] (insert (op :ins "a"))]
      (is (= op1 (op :ins "a")))
      (is (= op2 (op :ret 1))))))

(deftest retain-test
  (testing "retain-trans will return a vec of two retain operations of the same value"
    (let [[a b] (retain 5)]
      (is (= a (op :ret 5)))
      (is (= b (op :ret 5))))))

(deftest update-head-test
  (testing "update-head will change the :val of the head of the vec passed"
    (not (= (:val (first op-tom)) 4))
    (let [new-list (update-head op-tom 4)]
      (is (= (:val (first new-list)) 4)))))

(deftest transform-test
  (testing "Transforming two operations results in the correct pair of functions as response"
    (let [[a' b'] (transform op-tom op-jerry)
          expect-ops1' [(op :ret 2) (op :ins "a") (op :ret 1)]
          expect-ops2' [(op :ret 2) (op :ret 1) (op :ins "t")]]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2')))))