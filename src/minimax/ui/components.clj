(ns minimax.ui.components)

(def !current-element (atom nil))
(def !registry (atom {}))

(defn use-state [v]
  (when-not (contains? @!registry @!current-element)
    (swap! !registry assoc @!current-element (atom v)))
  (get @!registry @!current-element))

(defmacro defui [name args & body]
  `(defn ~name ~args
     (reset! !current-element ~(str (ns-name *ns*) "/" name))
     ~@body))
