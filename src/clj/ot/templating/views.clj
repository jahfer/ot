(ns ot.templating.views
  (:require [clojure.string :as str]
            [hiccup.page :as hic-p]))

(defn gen-page-head [title]
  [:head
   [:title title]
   (hic-p/include-css "/css/style.css")])

(def header-links
  [:div#header-links
   "[ "
   [:a {:href "/"} "Home"]
   " | "
   [:a {:href "/edit"} "Edit"]
   " ]"])

(defn document-page []
  (hic-p/html5
   (gen-page-head (str "OT Editor"))
   [:body
    [:div#app]
    (hic-p/include-js "/js/vendor/react-0.8.0.js")
    (hic-p/include-js "/js/vendor/jquery-1.10.2.min.js")
    (hic-p/include-js "/js/dev/main.js")
    [:script "goog.require('ot.core')"]]))

(defn iframed-test []
  (hic-p/html5
   (gen-page-head "OT Editor - Testing")
   [:body
    [:iframe {:src "http://localhost:3000/editor" :class "test-editor"}]
    [:iframe {:src "http://localhost:3000/editor" :class "test-editor"}]]))
