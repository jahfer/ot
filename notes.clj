;; Editor fails on rebasing ret+del/del+ret...
;; something in the gen-inverse-ops trying to access a property like ISeq, but is Long??

;; IllegalArgumentException Don't know how to create ISeq from: java.lang.Long  clojure.lang.RT.seqFrom (RT.java:505)

;; ---

;; Ideas for multiple cursors
;; - Store as metadata
;; - Store as operation (is transient, so may not make sense)
;; - Pass as completely separate feed (seems weird b/c dependent on positions)

^{::cursor {:position 4 :name "Jahfer"}} {:ops [] :client-id "client-a9d9h-a3" :parent-id 4 :id 5}

;; ---

;; How to Render
;; Use react-style components for all nodes, and track edits within and between!
;; Text changes happen in a <OTText> element, images are <OTImage>,
;; ...cursors are potentially a separate layer on top?

;; Parse to data structure, render however you want!
;; Keep data structure around to apply operations onto? Sounds tricky, but necessary
;; The key is to represent your document as data!!!!

;; Good idea: B+ Tree for applying operations on data structure.
;; That way we can skip large chunks of the document to apply operations to
;; the correct area.