(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.routes :as routes]
            [ot.components.app :as app]
            [cljs.core.async :refer [chan <!]]
            [secretary.core :as secretary]
            [ot.lib.queue2 :as q2]
            [othello.documents :as documents]) ;; temp
  (:use-macros [jayq.macros :only [ready let-ajax]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(enable-console-print!)

(defn message-queue []
  (q2/new-queue "ws://localhost:3000/editor/ws"))

(def app-state (atom {:environment "development"
                      :navigation-point nil
                      :navigation-data nil
                      :settings {}
                      :comms    {:nav (chan)
                                 :queue (message-queue)}
                      :editor   {:authors {:current-user :123
                                           :cursors {}}
                                 :document-tree [{:id 1
                                                  :node-type ::documents/text
                                                  :length 7
                                                  :data "Hello, "}
                                                 {:id 2
                                                  :node-type :link
                                                  :length 6
                                                  :data {:href "http://jahfer.com"
                                                         :text "Jahfer"
                                                         :alt "Jahfer Husain's Portfolio"}}
                                                 {:id 3
                                                  :node-type ::documents/text
                                                  :length 20
                                                  :data "! How are you today?"}]}}))

(defn install-om [state container]
  (om/root app/app state {:target container}))

(defn main [state]
  (let [nav-ch (get-in @state [:comms :nav])]
    (go-loop []
      (let [[nav-point args] (<! nav-ch)]
        (swap! state (fn [s] (-> s
                                 (assoc :navigation-point nav-point)
                                 (assoc :navigation-data  args))))
        (recur)))
    (routes/define-routes! state)
    (secretary/dispatch! (.-pathname js/location))
    (if-let [container (.getElementById js/document "app")]
      (install-om state container)
      (throw (js/Error. "Container #app not found!")))))

(ready [] (main app-state))
