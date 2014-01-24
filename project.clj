(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [digest "1.4.3"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.2"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [com.h2database/h2 "1.3.170"]
                 [jayq "2.5.0"]
                 [prismatic/dommy "0.1.2"]
                 [fogus/ring-edn "0.2.0"]
                 [om "0.2.3"]
                 [com.facebook/react "0.8.0.1"]
                 [ring/ring-devel "1.1.8"]
                 [ring/ring-core "1.1.8"]
                 [http-kit "2.1.16"]]

  :main ot.core

  :plugins [[lein-cljsbuild "1.0.1"]
            [lein-ring "0.8.10"]]

  :ring {:handler ot.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}

  :source-paths ["src"]

  :cljsbuild {:crossovers [ot.crossover]
              :builds [{:id "dev"
               :source-paths ["src/ot/cljs"]
               :crossover-path "crossover-cljs"
               :crossover-jar false
               :compiler {
                 :output-to "resources/public/js/cljs.js"
                 :output-dir "resources/public/out"
                 :optimizations :none
                 :source-map true}}]})
