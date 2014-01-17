(ns ot.documents
  (:require [ot.transforms :as transforms]))

(require 'digest)
(use '[clojure.string :only (join)])

(defn doc-id [contents]
  (digest/md5 contents))

(defn apply-ins [trans doc]
  (str (:val trans) doc))

(defn apply-ret [trans doc]
  (let [n (trans :val)
        head (take n doc)
        tail (nthrest doc n)]
    [(join head) (join tail)]))

(defn apply-ops [ops doc]
  (let [operation (first ops)]
    (cond
     (transforms/insert? operation)
       (recur (rest ops) (apply-ins operation doc))
     (transforms/retain? operation)
       (let [[head tail] (apply-ret operation doc)]
         (str head (apply-ops (rest ops) tail)))
     :else doc)))