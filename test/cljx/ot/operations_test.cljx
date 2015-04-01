(ns ot.operations-test
  (:require #+clj [clojure.test :as t :refer (is deftest testing)]
            #+cljs [cemerick.cljs.test :as t :refer-macros [is deftest testing]]
            [ot.operations :as operations]))

(def document "go")
(def op-tom (operations/oplist :ret 2 :ins "a"))

(deftest oplist-test
  (testing "Produces a vector of Operations"
    (let [ops (operations/oplist :ret 5 :ins "a" :ret 3)]
      (is (= ops [(operations/->Op :ret 5) (operations/->Op :ins "a") (operations/->Op :ret 3)])))))
