(ns ot.operations-test
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.operations :as operations])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

(def document "go")
(def op-tom (operations/oplist :ret 2 :ins "a"))

(deftest oplist-test
  (testing "Produces a vector of Operations"
    (let [ops (operations/oplist :ret 5 :ins "a" :ret 3)]
      (is (= ops [(operations/->Op :ret 5) (operations/->Op :ins "a") (operations/->Op :ret 3)])))))
