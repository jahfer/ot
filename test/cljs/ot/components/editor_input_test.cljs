(ns ot.components.editor-input-test
  (:require [cemerick.cljs.test :as t]
            [dommy.core :as dommy]
            [om.core :as om :include-macros true]
            [ot.lib.test-util :as util]
            [ot.components.editor-input :as editor-input])
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(deftest editor-renders?
  (let [data {:caret 0 :text "Foobar"}]
    (testing "Correct editor contents"
      (is (= "Foobar"
             (let [c (util/new-container!)]
               (print "Testing with container: " c)
               (om/root editor-input/editor-input data {:target c})
               (dommy/text (sel1 c :textarea#editor))))))))
