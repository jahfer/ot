(ns ot.crossover.documents-test
  (:require [clojure.test :refer :all]
            [ot.crossover.transforms :refer :all]
            [ot.crossover.documents :refer :all]))

(def document "ram")

(def op-tom
  [(op :ret 1) (op :ins "o") (op :ret 2) (op :ins "!")])

(deftest apply-ins-test
  (testing "Applying an insert operation prepends the character to the document"
    (let [ins-op (op :ins "g")
          out (apply-ins ins-op document)]
      (is (= out "gram")))))

(deftest apply-ret-test
  (testing "Applying a retain operation splits the document at the retain count"
    (let [ret-op (op :ret 2)
          [head tail] (apply-ret ret-op "hello")]
      (is (= head "he"))
      (is (= tail "llo")))))

(deftest apply-ops-test
  (testing "Applying a series of operations results in the correct end document"
    (let [result (apply-ops document op-tom)]
      (is (= result "roam!")))))
