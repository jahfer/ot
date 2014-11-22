(ns ot.lib.test-util
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy :refer-macros [sel1]]
            [om.core :as om :include-macros true]))

(defn container-div []
  (let [id (str "container-" (gensym))]
    [(node [:div {:id id}]) (str "#" id)]))

(defn insert-container! [container]
  (dommy/append! (sel1 js/document :body) container))

(defn new-container! []
  (let [[n s] (container-div)]
    (insert-container! n)
    (sel1 s)))
