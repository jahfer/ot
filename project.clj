(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.1"]]

  :main puppetlabs.trapperkeeper.main

  :plugins [[com.keminglabs/cljx "0.5.0"]]

  :ring {:handler ot.handler/app}

  :profiles {:default [:base :system :user :provided :clj]

             :clj {:dependencies [[ring/ring-core "1.3.1"]
                                  [compojure "1.2.1"]
                                  [http-kit "2.1.18"]
                                  [hiccup "1.0.5"]
                                  [joplin.core "0.2.4"]
                                  [clojurewerkz/cassaforte "2.0.0"]
                                  [com.cognitect/transit-clj "0.8.259"]
                                  [org.clojure/tools.nrepl "0.2.3"]
                                  [org.clojure/tools.cli "0.3.1"]
                                  [puppetlabs/trapperkeeper "1.0.1"]
                                  [puppetlabs/trapperkeeper "1.0.1" :classifier "test" :scope "test"]
                                  [javax.servlet/servlet-api "2.5"]
                                  [org.clojure/tools.logging "0.2.6"]
                                  [org.clojure/tools.namespace "0.2.4"]]
                   :plugins [[joplin.lein "0.2.4"]]
                   :source-paths ["target/generated/src/clj" "src/clj" "db"]
                   :test-paths   ["target/generated/test/clj" "test/clj"]
                   :resource-paths ["resources" "target/generated/src/cljs"]

                   :joplin {:migrators {:cass-mig "db/migrators/cass"}
                            :databases {:cass-dev {:type :cass
                                                   :hosts ["localhost"]
                                                   :keyspace "ot_dev"}}
                            :environments {:dev [{:db :cass-dev :migrator :cass-mig}]}}}

             :cljs {:dependencies [[org.clojure/clojurescript "0.0-2740"]
                                   [jayq "2.5.2"]
                                   [prismatic/dommy "1.0.0"]
                                   [org.omcljs/om "0.8.7"]
                                   [cljsjs/react "0.12.2-5"]
                                   [com.cognitect/transit-cljs "0.8.192"]]
                    :plugins [[lein-cljsbuild "1.0.3"]
                              [com.cemerick/clojurescript.test "0.3.1"]]
                    :cljsbuild {:builds {:dev {:preamble ["react/react.min.js"]
                                               :source-paths ["src/cljs"
                                                              "test/cljs"
                                                              "target/generated/src/cljs"
                                                              "target/generated/test/cljs"]
                                               :compiler {:output-to "resources/public/js/out/main.js"
                                                          :output-dir "resources/public/js/out"
                                                          :optimizations :whitespace
                                                          :pretty-print true
                                                          :source-map "resources/public/js/out/main.js.map"}}}
                                :test-commands {"unit-tests" ["slimerjs" :runner
                                                              "resources/public/js/vendor/jquery-1.10.2.min.js"
                                                              "resources/public/js/out/main.js"]}}}}

  :aliases {"server" ["trampoline" "run" "--bootstrap-config" "resources/bootstrap.cfg" "--config" "resources/config.conf"]
            "client" ["with-profile" "cljs" "cljsbuild" "auto" "dev"]
            "clj-test" ["with-profile" "clj" "test"]
            "cljs-repl" ["with-profile" "cljs" "trampoline" "cljsbuild" "repl-listen"]
            "cljs-test" ["with-profile" "cljs" "cljsbuild" "test"]
            "clj-clean-test" ["do" "clean," "clj-test"]
            "cljs-clean-test" ["do" "clean," "cljs-test"]
            "all-tests" ["do" "with-profile" "clj" "test," "with-profile" "cljs" "cljsbuild" "test"]}

  :repl-options {:init-ns ot.repl}

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
