(ns ot.components.editor-test
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy :refer-macros [sel1]]
            [om.core :as om :include-macros true]
            [ot.lib.test-util :as util]
            [ot.operations :as operations]
            [ot.transforms :as transforms]
            [ot.components.editor :as editor])
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest testing done)]))

(enable-console-print!)

(defn build-editor-view [init-state]
  (fn [app owner]
    (reify
      om/IRender
      (render [_]
        (om/build editor/editor-view (:editor app)
                  {:init-state init-state})))))

(deftest ^:async editor-renders?
  (let [data {:editor {:local-id 123 :owned-ids []}}
        init-state {:text "Foobar" :parent-id 3}]
    (testing "Correct editor contents"
      (is (= "Foobar"
             (let [c (util/new-container!)]
               (om/root (build-editor-view init-state) data {:target c})
               (dommy/text (sel1 :textarea#editor)))))
      (done))))

(deftest editor-reacts?
  (let [data {:text "Foobar"}
        caret-position 6]
    (testing "gen-insert-op returns a correct description of the user input"
      (is (= (operations/oplist :ret 6 :ins "!")
             (editor/gen-insert-op "!" caret-position (:text data)))))))
