(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [digest "1.4.3"]
                 [org.clojure/clojurescript "0.0-2127"]
                 [org.clojure/core.async "0.1.256.0-1bf8cf-alpha"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [com.h2database/h2 "1.3.170"]
                 [cljs-ajax "0.2.3"]
                 [fogus/ring-edn "0.2.0"]]

  :plugins [[lein-cljsbuild "1.0.1"]
            [lein-ring "0.8.10"]]

  :ring {:handler ot.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}

  :source-paths ["src"]

  :cljsbuild {
     :builds [{:id "ot"
               :source-paths ["src/ot/cljs"]
               :compiler {
                 :output-to "resources/public/js/cljs.js"
                 :output-dir "resources/public/out"
                 :optimizations :none
                 :source-map true}}]})
