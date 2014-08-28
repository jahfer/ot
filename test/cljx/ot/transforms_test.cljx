(ns ot.transforms-test
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.transforms :as transforms])
  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

(def document "go")

(def op-tom   (transforms/oplist :ret 2 :ins "a"))
(def op-jerry (transforms/oplist :ret 2 :ins "t"))
(def op-barry (transforms/oplist :ret 1 :del 1))

(def tom-document
  (str document "a"))
(def jerry-document
  (str document "t"))

(deftest oplist-test
  (testing "Produces a vector of Operations"
    (let [ops (transforms/oplist :ret 5 :ins "a" :ret 3)]
      (is (= ops [(transforms/->Op :ret 5) (transforms/->Op :ins "a") (transforms/->Op :ret 3)])))))

(deftest insert-test
  (testing "An insert operation will produce the correct resulting pair"
    (let [[op1 op2] (transforms/insert (transforms/->Op :ins "a"))]
      (is (= op1 (transforms/->Op :ins "a")))
      (is (= op2 (transforms/->Op :ret 1))))))

(deftest retain-test
  (testing "retain-trans will return a vec of two retain operations of the same value"
    (let [[a b] (transforms/retain 5)]
      (is (= a (transforms/->Op :ret 5)))
      (is (= b (transforms/->Op :ret 5))))))

(deftest update-head-test
  (testing "update-head will change the :val of the head of the vec passed"
    (not (= (:val (first op-tom)) 4))
    (let [new-list (transforms/update-head op-tom 4)]
      (is (= (:val (first new-list)) 4)))))

(deftest transform-test
  (testing "Transforming two operations results in the correct pair of functions as response"
    (let [[a' b'] (transforms/transform op-tom op-jerry)
          expect-ops1' (transforms/oplist :ret 2 :ins "a" :ret 1)
          expect-ops2' (transforms/oplist :ret 3 :ins "t")]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2')))))

(deftest delete-test
  (testing "Transforming a delete operation with an insert operation results in the correct pair of operations"
    (let [[a' b'] (transforms/transform op-tom op-barry)
          expect-ops1' (transforms/oplist :ret 1 :ins "a")
          expect-ops2' (transforms/oplist :ret 1 :del 1 :ret 1)]
      (is (= a' expect-ops1'))
      (is (= b' expect-ops2'))))
  (testing "Transforming an insert operation with a delete operation results in the correct pair of operations"
    (let [[c' d'] (transforms/transform op-barry op-tom)
          expect-ops3' (transforms/oplist :ret 1 :del 1 :ret 1)
          expect-ops4' (transforms/oplist :ret 1 :ins "a")]
      (is (= c' expect-ops3'))
      (is (= d' expect-ops4'))))
  (testing "Tranforming two delete operations results in the correct pair of operations"
    (let [[e' f'] (transforms/transform op-barry op-barry)
          expect-ops5' (transforms/oplist :ret 1)
          expect-ops6' (transforms/oplist :ret 1)]
      (is (= e' expect-ops5'))
      (is (= f' expect-ops6')))))

(deftest compress-test
  (testing "#compress will join all neighboring like items"
    (let [ops1 (transforms/oplist :ret 2 :ret 1 :ins "a" :ret 1 :ret 3)
          ops2 (transforms/oplist :ret 1 :ret 1 :ret 1 :ret 1 :ins "b")
          result1 (transforms/compress ops1)
          result2 (transforms/compress ops2)]
      (is (= result1 (transforms/oplist :ret 3 :ins "a" :ret 4)))
      (is (= result2 (transforms/oplist :ret 4 :ins "b"))))))

(deftest compose-test
  (testing "Composing two lists of operations results in a single list of all combined operations"
    (let [comp (transforms/compose (transforms/oplist :ret 1 :ins "o") (transforms/oplist :ret 2 :ins "t"))]
      (is (= comp (transforms/oplist :ret 1 :ins "o" :ins "t")))))
  (testing "Composing a delete with an insert returns the expected result"
    (let [comp (transforms/compose (transforms/oplist :ret 1 :ins "o") (transforms/oplist :ret 2 :del 1))]
      (is (= comp (transforms/oplist :ret 1 :ins "o" :del 1))))))
