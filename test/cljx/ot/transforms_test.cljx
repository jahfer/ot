(ns ot.transforms-test
  (:use [ot.transforms :only [transform compress insert retain]])
  (:require #+clj [clojure.test :as t :refer (is deftest testing)]
            #+clj [clojure.test.check.properties :as prop]
            #+cljs [cljs.test.check.properties :as prop :refer-macros [for-all]]
            #+clj [clojure.test.check.clojure-test :refer [defspec]]
            #+cljs [cljs.test.check.cljs-test :refer-macros [defspec]]
            [ot.lib.test-check-helper :as tch]
            [ot.operations :as operations :refer [oplist ->Op]]
            [ot.documents :as documents]))

(def document "go")

(def op-tom   (oplist :ret 2 :ins "a"))
(def op-jerry (oplist :ret 2 :ins "t"))
(def op-barry (oplist :ret 1 :del 1))

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(defn assert-transforms [doc a b expected-a' expected-b']
  "Asserts that transform return the expected values, and fulfills the commutative expectation"
  (let [doc-a (documents/apply-ops doc a)
        doc-b (documents/apply-ops doc b)
        [a' b'] (transform a b)]
    (is (= a' expected-a'))
    (is (= b' expected-b'))
    (is (= (documents/apply-ops doc-a b') (documents/apply-ops doc-b a')))))

(deftest testcheck-regression-tests
  (testing "multi-char delete + unequal-length retain regression"
    (let [a (oplist :ret 1 :ins "i" :ret 2)
          b (oplist :del 2 :ret 1)
          expected-a' (oplist :ins "i" :ret 1)
          expected-b' (oplist :del 1 :ret 1 :del 1 :ret 1)]
      (assert-transforms "hya" a b expected-a' expected-b')))
  (testing "competing deletes of different length regression"
    (let [a (oplist :del 2 :ret 1)
          b (oplist :del 1 :ret 2)
          expected-a' (oplist :del 1 :ret 1)
          expected-b' (oplist :ret 1)]
      (assert-transforms "hey" a b expected-a' expected-b'))))

(defspec transform-works-with-even-length-operations
  100
  (prop/for-all [[a b] tch/oplist-pair-gen]
                (let [doc (clojure.string/join (take (reduce + (map tch/oplen a)) (repeat "a")))
                      doc-a (documents/apply-ops doc a)
                      doc-b (documents/apply-ops doc b)
                      [a' b'] (transform a b)]
                  (= (documents/apply-ops doc-a b') (documents/apply-ops doc-b a')))))

(deftest insert-test
  (testing "insert will produce the correct resulting pair"
    (let [[op1 op2] (insert (operations/->Op :ins "a"))]
      (is (= op1 (->Op :ins "a")))
      (is (= op2 (->Op :ret 1))))))

(deftest retain-test
  (testing "retain will return a vec of two retain operations of the same value"
    (let [[a b] (retain 5)]
      (is (= a (->Op :ret 5)))
      (is (= b (->Op :ret 5))))))

(deftest transform-test
  (testing "Transforming two operations"
    (let [expected-a' (oplist :ret 2 :ins "a" :ret 1)
          expected-b' (oplist :ret 3 :ins "t")]
            (assert-transforms "Hi" op-tom op-jerry expected-a' expected-b'))))

(deftest delete-test
  (testing "Transforming a delete operation with an insert operation"
    (let [expected-a' (oplist :ret 1 :ins "a")
          expected-b' (oplist :ret 1 :del 1 :ret 1)]
      (assert-transforms "Hi" op-tom op-barry expected-a' expected-b')))

  (testing "Transforming an insert operation with a delete operation"
    (let [expected-a' (oplist :ret 1 :del 1 :ret 1)
          expected-b' (oplist :ret 1 :ins "a")]
      (assert-transforms "Hi" op-barry op-tom expected-a' expected-b')))

  (testing "Tranforming two delete operations"
    (let [expected-a' (oplist :ret 1)
          expected-b' (oplist :ret 1)]
      (assert-transforms "Hi" op-barry op-barry expected-a' expected-b')))

  (testing "Transforming deletes at different points of the same document"
    (let [a (oplist :ret 1 :del 1 :ret 2)
          b (oplist :ret 2 :del 1 :ret 1)
          expected-a' (oplist :ret 1 :del 1 :ret 1)
          expected-b' (oplist :ret 1 :del 1 :ret 1)]
      (assert-transforms "Hi?!" a b expected-a' expected-b'))))

(deftest compress-test
  (testing "#compress will join all neighboring like items"
    (let [ops1 (oplist :ret 2 :ret 1 :ins "a" :ret 1 :ret 3)
          ops2 (oplist :ret 1 :ret 1 :ret 1 :ret 1 :ins "b")
          result1 (compress ops1)
          result2 (compress ops2)]
      (is (= result1 (oplist :ret 3 :ins "a" :ret 4)))
      (is (= result2 (oplist :ret 4 :ins "b"))))))
