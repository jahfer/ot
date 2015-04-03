(ns ot.composers-test
  (:use [ot.composers :only [compose]])
  (:require #+clj [clojure.test :as t :refer (is deftest testing)]
            #+cljs [cemerick.cljs.test :as t :refer-macros [is deftest testing]]
            [ot.operations :as operations :refer [oplist]]))

(deftest basic-compose-test
  (testing ":del _"
    (let [comp (compose (oplist :del 1) (oplist :ins "a"))]
      (is (= comp (oplist :del 1 :ins "a")))))
   (testing "_ :ins"
    (let [comp (compose (oplist :ret 1) (oplist :ins "a" :ret 1))]
      (is (= comp (oplist :ins "a" :ret 1)))))
   (testing ":ret :ret"
     (let [comp (compose (oplist :ret 1 :ret 1) (oplist :ret 2))]
       (is (= comp (oplist :ret 1 :ret 1)))))
   (testing ":ins :ret"
     (let [comp (compose (oplist :ins "a" :ret 1) (oplist :ret 2 :ins "b"))]
       (is (= comp (oplist :ins "a" :ret 1 :ins "b")))))
   (testing ":ins :del"
     (let [comp (compose (oplist :ins "a" :ret 1) (oplist :del 1 :ret 1))]
       (is (= comp (oplist :ret 1)))))
   (testing ":ret :del"
     (let [comp (compose (oplist :ret 1) (oplist :del 1))]
       (is (= comp (oplist :del 1))))))

(deftest compose-test
  (testing "Composing two lists of operations results in a single list of all combined operations"
    (let [comp (compose (oplist :ret 1 :ins "o") (oplist :ret 2 :ins "t"))]
      (is (= comp (oplist :ret 1 :ins "o" :ins "t")))))
  (testing "Composing a delete with an insert"
    (let [comp (compose (oplist :ret 1 :ins "o") (oplist :ret 2 :del 1))]
      (is (= comp (oplist :ret 1 :ins "o" :del 1)))))
  (testing "Composing two delete actions"
    (let [comp (compose (oplist :ret 3 :del 1) (oplist :ret 2 :del 1))]
      (is (= comp (oplist :ret 2 :del 1 :del 1))))))
