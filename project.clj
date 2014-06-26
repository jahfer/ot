(def tk-version "0.4.2")
(def ks-version "0.5.3")

(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :main puppetlabs.trapperkeeper.main

  :plugins [[com.keminglabs/cljx "0.4.0"]]

  :ring {:handler ot.handler/app}
  :profiles {:clj {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]
                                  [puppetlabs/trapperkeeper ~tk-version :classifier "test" :scope "test"]
                                  [puppetlabs/kitchensink ~ks-version :classifier "test" :scope "test"]
                                  [ring-mock "0.1.5"]
                                  [log4j/log4j "1.2.16"]
                                  [hiccup "1.0.5"]
                                  [org.clojure/java.jdbc "0.2.3"]
                                  [com.h2database/h2 "1.3.170"]
                                  [fogus/ring-edn "0.2.0"]
                                  [ring/ring-devel "1.1.8"]
                                  [ring/ring-core "1.1.8"]
                                  [compojure "1.1.6"]
                                  [http-kit "2.1.16"]
                                  [puppetlabs/trapperkeeper ~tk-version]
                                  [org.clojure/tools.logging "0.2.6"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["target/generated/src/clj" "src/clj"]
                   :test-paths ["target/generated/test/clj" "test/clj"]
                   :resource-paths ["resources" "target/generated/src/cljs"]}

             :cljs {:dependencies [[org.clojure/clojurescript "0.0-2197"]
                                   [jayq "2.5.1"]
                                   [prismatic/dommy "0.1.2"]
                                   [om "0.6.4"]
                                   [com.facebook/react "0.8.0.1"]]
                    :plugins [[lein-cljsbuild "1.0.3"]]
                    :cljsbuild {:builds {:dev { :source-paths ["src/cljs" "target/generated/src/cljs"]
                                                :compiler {:output-to "resources/public/js/cljs.js"
                                                           :output-dir "resources/public/out"
                                                           :optimizations :none
                                                           :source-map true}}}}}}

  :aliases {"tk" ["do" "cljx," "with-profile" "clj" "trampoline" "run" "--bootstrap-config" "resources/bootstrap.cfg" "--config" "resources/config.conf"]
            "cljsc" ["with-profile" "cljs" "trampoline" "cljsbuild" "repl-listen"]
            "cljsb" ["do" "cljx," "with-profile" "cljs" "cljsbuild" "auto" "dev"]}

  :hooks [cljx.hooks]

  :repl-options {:init-ns user}

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/src/clj"
                   :rules :clj}
                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/src/cljs"
                   :rules :cljs}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/clj"
                   :rules :clj}
                  {:source-paths ["test/cljx"]
                   :output-path "target/generated/test/cljs"
                   :rules :cljs}]})
