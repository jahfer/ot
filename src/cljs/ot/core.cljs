(ns ot.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.routes :as routes]
            [ot.components.app :as app]
            [cljs.core.async :refer [chan <!]]
            [secretary.core :as secretary]
            [ot.lib.queue2 :as q2]
            cljsjs.react)
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
                      :editor   {}}))

(defn install-om [state container]
  (om/root app/app state {:target container}))

(defn main [state]
  (let [container (.getElementById js/document "app")
        nav-ch (get-in @state [:comms :nav])]
    (go-loop []
      (let [[nav-point args] (<! nav-ch)]
        (swap! state (fn [s] (-> s
                                 (assoc :navigation-point nav-point)
                                 (assoc :navigation-data  args))))
        (recur)))
    (routes/define-routes! state)
    (secretary/dispatch! (.-pathname js/location))
    (install-om state container)))

(ready [] (main app-state))
