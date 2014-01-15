(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [digest "1.4.3"]
                 [org.clojure/clojurescript "0.0-2127"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]]

  :plugins [[lein-cljsbuild "1.0.1"]]

  :source-paths ["src"]

  :cljsbuild {
     :builds [{:id "ot"
               :source-paths ["src"]
               :compiler {
                 :output-to "public/script.js"
                 :output-dir "out"
                 :optimizations :none
                 :source-map true}}]})
