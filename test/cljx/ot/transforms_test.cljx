(ns ot.transforms-test
  (:use [ot.transforms])
  (:require #+clj [clojure.test :as t
                   :refer (is deftest with-test run-tests testing)]
            #+cljs [cemerick.cljs.test :as t]
            [ot.operations :as operations :refer [oplist ->Op]]
            [ot.documents :as documents]

            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.core.match :refer [match]])

  #+cljs (:require-macros [cemerick.cljs.test
                           :refer (is deftest with-test run-tests testing test-var)]))

;; -----

(defn len [op]
  (match [op]
         [{:type :ret :val v}] v
         [{:type :ins :val v}] 0
         [{:type :del :val v}] v))

(def ins-op-gen
  (gen/fmap (partial apply ->Op)
            (gen/tuple (gen/return :ins)
                       gen/char-alpha)))

(def del-or-ret-op-gen
  (gen/fmap (partial apply ->Op)
            (gen/tuple (gen/elements [:ret :del])
                       (gen/such-that #(not= % 0) gen/pos-int))))

(def op-gen
  (gen/one-of [ins-op-gen del-or-ret-op-gen]))

(def oplist-gen
  (gen/not-empty (gen/vector op-gen)))

(def oplist-pair-gen
  (gen/fmap
   (fn [[list-a list-b :as lists]]
     (let [diff (- (reduce + (map len list-a)) (reduce + (map len list-b)))]
       (cond
         (= 0 diff)  (map compress lists)
         (pos? diff) (map compress [list-a (conj list-b (->Op :ret diff))])
         (neg? diff) (map compress [list-b (conj list-a (->Op :ret (- diff)))]))))
   (gen/tuple oplist-gen oplist-gen)))

(last (gen/sample oplist-pair-gen))

(transform (oplist :del 1 :ins \h :del 4 :ins \A :ret 8) (oplist :ret 1 :del 3 :ret 9))

(defspec transform-works-with-even-length-operations
  100
  (prop/for-all [[a b] oplist-pair-gen]
                (let [doc (clojure.string/join (take (reduce + (map len a)) (repeat "a")))
                      doc-a (documents/apply-ops doc a)
                      doc-b (documents/apply-ops doc b)
                      [a' b'] (transform a b)]
                  (= (documents/apply-ops doc-a b') (documents/apply-ops doc-b a')))))

;; -----

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
    (let [a (oplist :del 2 :ret 1) ; y
          b (oplist :del 1 :ret 2) ; ey
          expected-a' (oplist :del 1 :ret 1)
          expected-b' (oplist :ret 1)]
      (assert-transforms "hey" a b expected-a' expected-b')))) ; y

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
