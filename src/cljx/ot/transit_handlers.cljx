(ns ot.transit-handlers
  (:require [cognitect.transit :as t]
            [ot.transforms :as transforms]))

(def op-read-handler
  (t/read-handler #(apply transforms/->Op %)))

(def op-write-handler
  (t/write-handler
   (fn [_] "op")
   (fn [op] [(:type op) (:val op)])))

(def read-handlers {"op" op-read-handler})
(def write-handlers {ot.transforms.Op op-write-handler})
