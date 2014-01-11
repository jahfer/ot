(ns ot.core-test
  (:require [clojure.test :refer :all]
            [ot.core :refer :all])
  (:use [clojure.data.finger-tree :only [double-list]]))

(def document "go")

(def op-tom
  (double-list (op :ret 2) (op :ins "a")))
(def op-jerry
  (double-list (op :ret 2) (op :ins "t")))

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(deftest transform-test
  (testing "Transforming two operations results in the correct pair of functions as response"
    (let [[a' b'] (transform op-tom op-jerry)
          expect-ops1' (double-list (op :ret 2) (op :ins "a") (op :ret 1))
          expect-ops2' (double-list (op :ret 2) (op :ret 1) (op :ins "t"))]
      (is (seq-equals a' expect-ops1'))
      (is (seq-equals b' expect-ops2')))))