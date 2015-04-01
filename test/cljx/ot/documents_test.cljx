(ns ot.documents-test
  (:require #+clj [clojure.test :as t :refer (is deftest testing)]
            #+cljs [cemerick.cljs.test :as t :refer-macros [is deftest testing]]
            [ot.operations :as operations]
            [ot.documents :as documents]))

(def document "ram")

(def op-tom
  [(operations/->Op :ret 1)
   (operations/->Op :ins "o")
   (operations/->Op :ret 2)
   (operations/->Op :ins "!")])

(def op-jerry
  (operations/oplist :del 1 :ins "R" :ret 2))

(deftest apply-ins-test
  (testing "Applying an insert operation prepends the character to the document"
    (let [ins-op (operations/->Op :ins "g")
          out (documents/apply-ins ins-op document)]
      (is (= out "gram")))))

(deftest apply-ret-test
  (testing "Applying a retain operation splits the document at the retain count"
    (let [ret-op (operations/->Op :ret 2)
          {:keys [head tail]} (documents/apply-ret ret-op "hello")]
      (is (= head "he"))
      (is (= tail "llo")))))

(deftest apply-ops-test
  (testing "Applying a series of operations results in the correct end document"
    (let [result (documents/apply-ops document op-tom)]
      (is (= result "roam!"))))
  (testing "Applying a delete operation results in the correct end document"
    (let [result (documents/apply-ops document op-jerry)]
      (is (= result "Ram")))))
