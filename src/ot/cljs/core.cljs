(ns ot.cljs.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [jayq.util :as jq-util]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ot.cljs.lib.sockets :as ws]
            [ot.cljs.components.editor :as editor])
  (:use-macros [dommy.macros :only [node sel sel1]]))

;; Define intial state of app
(def app-state (atom {}))

;; Entrance point
(defn ot-app [app owner]
  (reify
    om/IWillMount
    (will-mount [_]
                (let [comm (chan)]
                  (om/set-state! owner :comm comm)))
    om/IWillUpdate
    (will-update [_ _ _])
    om/IDidUpdate
    (did-update [_ _ _])
    om/IRender
    (render [_]
            (let [comm (om/get-state owner :comm)]
              (om/build editor/editor-view app
                        {:opts {:comm comm}})))))

;; Let's kick things off
(om/root ot-app app-state
         {:target (sel1 :#app)})
