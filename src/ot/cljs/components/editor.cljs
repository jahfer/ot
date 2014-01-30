(ns ot.cljs.components.editor
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [ot.cljs.util.sockets :as ws]
            [ot.crossover.transforms :as transforms]
            [ot.cljs.util.misc :as util])
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:use-macros [dommy.macros :only [node sel sel1]]))

(def confirmation (chan))
(def queue (chan))
(def outbound (chan))
(def owned-ids [])

(defn component [app owner opts]
  (reify
    om/IInitState
    (init-state [_]
                (.log js/console "Initializing editor"))
    om/IWillMount
    (will-mount [_]
                (ws/init!)
                (ws/event-chan queue (sel1 :#editor) :keypress))
    om/IDidUpdate
    (did-update [_ _ _ _])
    om/IRenderState
    (render-state [data owner]
      (let [{:keys [comm]} opts]
        (dom/textarea #js {:ref "editor"
                           :id "editor"
                           :defaultValue (-> app :editor :text)
                           :onKeyPress #(put! queue (.-key %))})))))

(defn outbound-queue
  "Routine for sending commands to the server. Converts
  to an operation, and transforms to the literal string
  version before sending."
  []
  (.log js/console "[outbound-q] Initializing")
  (go (while true
        (let [key (<! outbound)]
          (.log js/console "[outbound-q] Sending operation to the server")
          (-> (transforms/op :ins key)
              (pr-str)
              (ws/send))))))

(defn buffer-queue
  "Blocking routine that throttles outbound operations.
  Waits for confirmation on previous operation before
  sending the new operation."
  []
  (.log js/console "[buffer-q] Initializing")
  (go (while true
        (.log js/console "[buffer-q] Waiting on server confirmation")
        (let [_ (<! confirmation)]
          (.log js/console "[buffer-q] Received confirmation, waiting for outbound items")
          (let [out (<! queue)]
            (.log js/console "[buffer-q] Sending operation to outbound")
            (put! outbound out))))))

(defn inbound-queue
  "Routine to receive operations from server. When the
  ID is owned by the client, it is removed from the
  owned-ids queue, and added to the queue of confirmed
  operations."
  []
  (.log js/console "[inbound-q] Initializing")
  (go (while true
        (let [response (<! ws/recv)
              id (:id response)]
          (when (util/in? owned-ids id)
            (def owned-ids (remove #{id} owned-ids))
            (put! confirmation response))))))

(defn init []
  (put! confirmation true)
  (inbound-queue)
  (buffer-queue)
  (outbound-queue))

(init)
