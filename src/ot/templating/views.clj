(ns ot.templating.views
  (:require [ot.crossover.transforms :as transforms]
            [clojure.string :as str]
            [hiccup.page :as hic-p]))

(defn gen-page-head [title]
  [:head
   [:title title]
   [:script#lt_ws {:src "http://localhost:64352/socket.io/lighttable/ws.js"}]
   (hic-p/include-css "/css/style.css")])

(def header-links
  [:div#header-links
   "[ "
   [:a {:href "/"} "Home"]
   " | "
   [:a {:href "/edit"} "Edit"]
   " ]"])

(defn home-page []
  (hic-p/html5
   (gen-page-head "OT Editor")
   [:body
    [:div#app]
    (hic-p/include-js "http://fb.me/react-0.8.0.js")
    (hic-p/include-js "/out/goog/base.js")
    (hic-p/include-js "/js/vendor/md5.js")
    (hic-p/include-js "/js/vendor/jquery-1.10.2.min.js")
    (hic-p/include-js "/js/cljs.js")
    [:script "goog.require('ot.cljs.core')"]]))
