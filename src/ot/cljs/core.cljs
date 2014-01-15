(ns ot.cljs.core)


(defn foo [a b]
  (+ a b))

(. js/console (log "Hello world!" (foo 1 2)))