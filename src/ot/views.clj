(ns ot.views
  (:require [ot.transforms :as transforms]
            [clojure.string :as str]
            [hiccup.page :as hic-p]))

(defn gen-page-head [title]
  [:head
   [:title (str "Locations: " title)]
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
   (gen-page-head "Home")
   header-links
   [:h1 "Home"]
   [:p "Webapp to live-edit documents"]))

(defn edit-page []
  (hic-p/html5
   (gen-page-head "Edit")
   header-links
   [:h1 "Edit"]
   [:input#query {:type "text"}]
   [:button#search "Search"]
   [:p#results]
   [:hr]
   [:textarea#editor]
   (hic-p/include-js "/out/goog/base.js")
   (hic-p/include-js "/js/cljs.js")
   [:script "goog.require('ot.cljs.core')"]))

(defn live-update [ops]
  (map transforms/print-op ops))