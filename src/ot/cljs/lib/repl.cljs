(ns ot.lib.repl
  (:require [clojure.browser.repl :as repl]))

(enable-console-print!)

(println "Enabling repl")
(repl/connect "http://localhost:9000/repl")
