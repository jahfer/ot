(ns ot.composers-test
  (:require #+clj [clojure.test :as t :refer (is deftest testing)]
            #+cljs [cemerick.cljs.test :as t :refer-macros [is deftest testing]]
            [ot.operations :as o]
            [ot.composers :as composers]))

(deftest compose-test
  (testing "Composing two lists of operations results in a single list of all combined operations"
    (let [comp (composers/compose (o/oplist :ret 1 :ins "o") (o/oplist :ret 2 :ins "t"))]
      (is (= comp (o/oplist :ret 1 :ins "o" :ins "t")))))
  (testing "Composing a delete with an insert"
    (let [comp (composers/compose (o/oplist :ret 1 :ins "o") (o/oplist :ret 2 :del 1))]
      (is (= comp (o/oplist :ret 1 :ins "o" :del 1)))))
  (testing "Composing two delete actions"
    (let [comp (composers/compose (o/oplist :ret 3 :del 1) (o/oplist :ret 2 :del 1))]
      (is (= comp (o/oplist :ret 2 :del 1 :del 1))))))
