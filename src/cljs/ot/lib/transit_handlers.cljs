(ns ot.lib.transit-handlers
  (:require [cognitect.transit :as t]
            [ot.transforms :as transforms]))

(def op-write-handler 
  (t/write-handler
   (fn [_] "op") 
   (fn [op] [(:type op) (:val op)])))

(def op-writer 
  (t/writer :json {:handlers {ot.transforms/Op op-write-handler}}))


