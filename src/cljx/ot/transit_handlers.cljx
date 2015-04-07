(ns ot.transit-handlers
  (:require [cognitect.transit :as t]
            [othello.operations :as operations]))

(def op-read-handler
  (t/read-handler #(apply operations/->Op %)))

(def op-write-handler
  (t/write-handler
   (fn [_] "op")
   (fn [op] [(:type op) (:val op)])))

(def read-handlers {"op" op-read-handler})
(def write-handlers {othello.operations.Op op-write-handler})
