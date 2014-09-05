(ns ot.transforms-test
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.operations :as operations]
            [ot.transforms :as transforms])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

(def document "go")

(def op-tom   (operations/oplist :ret 2 :ins "a"))
(def op-jerry (operations/oplist :ret 2 :ins "t"))
(def op-barry (operations/oplist :ret 1 :del 1))

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(deftest insert-test
  (testing "An insert operation will produce the correct resulting pair"
    (let [[op1 op2] (transforms/insert (operations/->Op :ins "a"))]
      (is (= op1 (operations/->Op :ins "a")))
      (is (= op2 (operations/->Op :ret 1))))))

(deftest retain-test
  (testing "retain-trans will return a vec of two retain operations of the same value"
    (let [[a b] (transforms/retain 5)]
      (is (= a (operations/->Op :ret 5)))
      (is (= b (operations/->Op :ret 5))))))

(deftest transform-test
  (testing "Transforming two operations results in the correct pair of functions as response"
    (let [[a' b'] (transforms/transform op-tom op-jerry)
          expect-ops1' (operations/oplist :ret 2 :ins "a" :ret 1)
          expect-ops2' (operations/oplist :ret 3 :ins "t")]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2')))))

(deftest delete-test
  (testing "Transforming a delete operation with an insert operation results in the correct pair of operations"
    (let [[a' b'] (transforms/transform op-tom op-barry)
          expect-ops1' (operations/oplist :ret 1 :ins "a")
          expect-ops2' (operations/oplist :ret 1 :del 1 :ret 1)]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2'))))
  (testing "Transforming an insert operation with a delete operation results in the correct pair of operations"
    (let [[c' d'] (transforms/transform op-barry op-tom)
          expect-ops3' (operations/oplist :ret 1 :del 1 :ret 1)
          expect-ops4' (operations/oplist :ret 1 :ins "a")]
      (is (= c' expect-ops3'))
      (is (= d' expect-ops4'))))
  (testing "Tranforming two delete operations results in the correct pair of operations"
    (let [[e' f'] (transforms/transform op-barry op-barry)
          expect-ops5' (operations/oplist :ret 1)
          expect-ops6' (operations/oplist :ret 1)]
      (is (= e' expect-ops5'))
      (is (= f' expect-ops6')))))

(deftest compress-test
  (testing "#compress will join all neighboring like items"
    (let [ops1 (operations/oplist :ret 2 :ret 1 :ins "a" :ret 1 :ret 3)
          ops2 (operations/oplist :ret 1 :ret 1 :ret 1 :ret 1 :ins "b")
          result1 (transforms/compress ops1)
          result2 (transforms/compress ops2)]
      (is (= result1 (operations/oplist :ret 3 :ins "a" :ret 4)))
      (is (= result2 (operations/oplist :ret 4 :ins "b"))))))
