(def tk-version "0.4.2")
(def ks-version "0.5.3")

(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/core.match "0.2.1"]]

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
                                  [com.cognitect/transit-clj "0.8.247"]
                                  [org.clojure/tools.nrepl "0.2.3"]
                                  [puppetlabs/trapperkeeper ~tk-version]
                                  [org.clojure/tools.logging "0.2.6"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["target/generated/src/clj" "src/clj"]
                   :test-paths ["target/generated/test/clj" "test/clj"]
                   :resource-paths ["resources" "target/generated/src/cljs"]}

             :cljs {:dependencies [[org.clojure/clojurescript "0.0-2311"]
                                   [jayq "2.5.2"]
                                   [prismatic/dommy "0.1.3"]
                                   [om "0.6.4"]
                                   [com.facebook/react "0.8.0.1"]
                                   [com.cognitect/transit-cljs "0.8.182"]
                                   [cljs-hash "0.0.2"]]
                    :plugins [[lein-cljsbuild "1.0.3"]
                              [com.cemerick/clojurescript.test "0.3.1"]]
                    :cljsbuild {:builds {:dev {:source-paths ["src/cljs"
                                                              "test/cljs"
                                                              "target/generated/src/cljs"
                                                              "target/generated/test/cljs"]
                                               :compiler {:output-to "resources/public/js/main.js"
                                                          :output-dir "resources/public/out"
                                                          :optimizations :whitespace
                                                          :pretty-print true}}}
                                :test-commands {"unit-tests" ["slimerjs" :runner
                                                              "resources/public/js/vendor/react-0.8.0.js"
                                                              "resources/public/js/vendor/jquery-1.10.2.min.js"
                                                              "resources/public/js/main.js"]}}}}

  :aliases {"server" ["do" "cljx," "with-profile" "clj" "trampoline" "run" "--bootstrap-config" "resources/bootstrap.cfg" "--config" "resources/config.conf"]
            "client" ["do" "cljx," "with-profile" "cljs" "cljsbuild" "auto" "dev"]
            "cljs-repl" ["with-profile" "cljs" "trampoline" "cljsbuild" "repl-listen"]
            "clj-test" ["do" "cljx," "with-profile" "clj" "test"]
            "cljs-test" ["do" "cljx," "with-profile" "cljs" "cljsbuild" "test"]
            "clj-clean-test" ["do" "clean," "clj-test"]
            "cljs-clean-test" ["do" "clean," "cljs-test"]
            "all-tests" ["do" "clean," "cljx," "clj-test," "cljs-test"]}

;  :hooks [cljx.hooks]

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
