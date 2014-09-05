(ns ot.composers-test
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.operations :as operations]
            [ot.composers :as composers])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

(deftest compose-test
  (testing "Composing two lists of operations results in a single list of all combined operations"
    (let [comp (composers/compose (operations/oplist :ret 1 :ins "o") (operations/oplist :ret 2 :ins "t"))]
      (is (= comp (operations/oplist :ret 1 :ins "o" :ins "t")))))
  (testing "Composing a delete with an insert returns the expected result"
    (let [comp (composers/compose (operations/oplist :ret 1 :ins "o") (operations/oplist :ret 2 :del 1))]
      (is (= comp (operations/oplist :ret 1 :ins "o" :del 1))))))
