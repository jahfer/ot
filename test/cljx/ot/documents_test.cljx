(ns ot.documents-test
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.transforms :as transforms]
            [ot.documents :as documents])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

(def document "ram")

(def op-tom
  [(transforms/->Op :ret 1)
   (transforms/->Op :ins "o")
   (transforms/->Op :ret 2)
   (transforms/->Op :ins "!")])

(deftest apply-ins-test
  (testing "Applying an insert operation prepends the character to the document"
    (let [ins-op (transforms/->Op :ins "g")
          out (documents/apply-ins ins-op document)]
      (is (= out "gram")))))

(deftest apply-ret-test
  (testing "Applying a retain operation splits the document at the retain count"
    (let [ret-op (transforms/->Op :ret 2)
          [head tail] (documents/apply-ret ret-op "hello")]
      (is (= head "he"))
      (is (= tail "llo")))))

(deftest apply-ops-test
  (testing "Applying a series of operations results in the correct end document"
    (let [result (documents/apply-ops document op-tom)]
      (is (= result "roam!")))))
