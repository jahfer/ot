(ns ot.core
  (:use [clojure.data.finger-tree :only [double-list]]))

(require 'digest)

(defn doc-id [contents]
  (digest/md5 contents))

(defn op [type val]
  {:type type :val val})

;; (defn incl-trans [ins1 ins2]
;;   (let [p1 (:pos ins1) p2 (:pos ins2)
;;         sid1 (:owner ins1) sid2 (:owner ins2)]
;;     (cond
;;       (< p1 p2) ins1
;;       (and (= p1 p2) (< sid1 sid2)) ins1
;;       :else (update-in ins1 [:pos] inc))))

;; (defn excl-trans [ins1 ins2]
;;   (let [p1 (:pos ins1) p2 (:pos ins2)
;;         sid1 (:owner ins1) sid2 (:owner ins2)]
;;     (cond
;;       (< p1 p2) ins1
;;       (and (= p1 p2) (< sid1 sid2)) ins1
;;       :else (update-in ins1 [:pos] dec))))


;; (defn ot [op1 op2]
;;   [[(op :ret 1) (op :ins "t") (op :ret 1)]
;;    [(op :ins "a") (op :ret 1) (op :ret 1)]])

(defn ot [op1 op2]
  )

(defn trans [opList1 opList2]
  (ot opList1 opList2))

;; Testing

(def document "go")

(def op-tom
  ['(op :ret 2) '(op :ins "a")])
(def op-jerry
  ['(op :ret 2) '(op :ins "t")])


;; can't do this..have to pass individ ops to OT
(trans op-tom op-jerry)