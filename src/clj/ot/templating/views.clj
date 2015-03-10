(ns ot.templating.views
  (:require [clojure.string :as str]
            [hiccup.page :as hic-p]))

(defn gen-page-head [title]
  [:head
   [:title title]
   (hic-p/include-css "/css/style.css")])

(def om-scripts
  (hic-p/include-js "/js/vendor/react-0.8.0.js"
                    "/js/vendor/jquery-1.10.2.min.js"
                    "/js/dev/main.js"))

(defn document []
  (hic-p/html5
   (gen-page-head (str "OT Editor"))
   [:body
    [:div#app]
    om-scripts
    [:script "goog.require('ot.routes')"]]))

(defn iframed-test []
  (hic-p/html5
   (gen-page-head "OT Editor - Testing")
   [:body
    [:iframe {:src "http://localhost:3000/editor" :class "test-editor"}]
    [:iframe {:src "http://localhost:3000/editor" :class "test-editor"}]]))
