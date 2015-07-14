(defproject ot "0.1.0"
  :description "Implementation of Operational Transform in Clojure"
  :url "https://github.com/jahfer/ot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :plugins [[com.cemerick/clojurescript.test "0.3.3"]
            [com.keminglabs/cljx "0.6.0"]
            [lein-cljsbuild "1.0.5"]]

  :dependencies [;; general
                 [org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/tools.reader]]
                 ;;[com.cemerick/piggieback "0.1.5" :exclusions [org.clojure/tools.reader]] ; temporary for CLJX
                 [org.clojure/test.check "0.7.0"]
                 [othello "0.1.0-SNAPSHOT" :exclusions [org.clojure/tools.reader]]
                 ;; server
                 [ring/ring-core "1.4.0" :exclusions [org.clojure/tools.reader]]
                 [compojure "1.3.4"]
                 [http-kit "2.1.18"]
                 [hiccup "1.0.5"]
                 [joplin.core "0.2.7"]
                 [joplin.cassandra "0.2.7"]
                 [clojurewerkz/cassaforte "2.0.0" :exclusions [com.google.guava/guava]]
                 [com.cognitect/transit-clj "0.8.275"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/tools.cli "0.3.1"]
                 [puppetlabs/trapperkeeper "1.0.1"]
                 [puppetlabs/trapperkeeper "1.0.1" :classifier "test" :scope "test"]
                 [javax.servlet/servlet-api "2.5"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.namespace "0.2.9"]
                 ;; client
                 [org.clojure/clojurescript "0.0-3308"]
                 [jayq "2.5.4"]
                 [secretary "1.2.2"]
                 [prismatic/dommy "1.0.0"]
                 [org.omcljs/om "0.8.8"]
                 [com.cognitect/transit-cljs "0.8.220"]]

  :main puppetlabs.trapperkeeper.main

  :ring {:handler ot.handler/app}

  :repl-options {:init-ns ot.repl}

  :profiles {:dev {:source-paths ["target/generated/src/clj" "src/clj" "db"]
                   :test-paths   ["target/generated/test/clj" "test/clj"]
                   :resource-paths ["resources" "target/generated/src/cljs"]
                   :jvm-opts ["-Xverify:none"]
                   ;; :java-cmd "drip"
                   :env {:is-dev true}
                   :joplin {:migrators {:cass-mig "db/migrators/cass"}
                            :databases {:cass-dev {:type :cass
                                                   :hosts ["localhost"]
                                                   :keyspace "ot_dev"}}
                            :environments {:dev [{:db :cass-dev :migrator :cass-mig}]}}}}

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
                   :rules :cljs}]}

  :cljsbuild {:builds {:test {:source-paths ["src/cljs"
                                             "test/cljs"
                                             "target/generated/src/cljs"
                                             "target/generated/test/cljs"]
                              ;; :notify-command ["slimerjs" :cljs.test/runner
                              ;;                  "resources/public/js/vendor/jquery-1.10.2.min.js"
                              ;;                  "resources/public/js/dev/main.js"]
                              :compiler {:output-to "resources/public/js/dev/main.js"
                                         :output-dir "resources/public/js/dev"
                                         :optimizations :simple
                                         :source-map "resources/public/js/dev/main.js.map"}}}

              :test-commands {"unit" ["slimerjs" :runner
                                      "resources/public/js/vendor/jquery-1.10.2.min.js"
                                      "resources/public/js/dev/main.js"]}}

  :aliases {"server" ["do" "cljx" "once,"
                      "trampoline" "run"
                      "--bootstrap-config" "resources/bootstrap.cfg"
                      "--config" "resources/config.conf"]
            "client" ["do" "cljx" "once," "cljsbuild" "auto" "test"]
            "cleantest" ["do" "clean,"
                         "cljx" "once,"
                         "trampoline" "test,"
                         "cljsbuild" "test"]})
