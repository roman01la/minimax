(ns minimax.util.scene)

(defn add-parent-link [node]
  (doseq [child (:children node)]
    (vreset! (:parent child) node))
  node)
