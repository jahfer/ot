(ns ot.operations)

(defrecord Op [type val])

(defn oplist [& operations]
  (mapv #(apply ->Op %) (partition 2 operations)))

(defn conj-ops [op-lists new-ops]
  (map conj op-lists new-ops))

(defn assoc-op [new-ops ops]
  (conj-ops ops new-ops))

(defn insert? [operation]
  (= :ins (:type operation)))

(defn retain? [operation]
  (= :ret (:type operation)))

(defn delete? [operation]
  (= :del (:type operation)))