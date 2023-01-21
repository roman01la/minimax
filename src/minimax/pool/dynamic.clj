(ns minimax.pool.dynamic
  (:require [minimax.pool.core :as pool]))

(set! *warn-on-reflection* true)

;; memoizes allocated object on args at alloc time
;; TODO: add object update code path
(deftype DynamicResourcePool
  [^:volatile-mutable taken-items ^:volatile-mutable free-items
   max-size create-item free-item]
  pool/IResourcePool
  (alloc [this]
    (pool/alloc this []))
  (alloc [this args]
    ;; TODO: Fix max check
    (if (empty? free-items)
      (do
        (assert (>= max-size (count taken-items)) (str "Resource pool has reached max size of " max-size " items " (type (first taken-items))))
        (let [item (apply create-item args)]
          (set! taken-items (assoc taken-items args item))
          item))
      (if-let [item (get free-items args)]
        (do
          (set! free-items (dissoc free-items args))
          (set! taken-items (assoc taken-items args item))
          item)
        (let [item (apply create-item args)]
          (set! taken-items (assoc taken-items args item))
          item))))
  (free [this]
    (set! free-items (into free-items taken-items))
    (set! taken-items {}))
  (destroy [this]
    (run! (fn [[_ item]] (free-item item)) taken-items)
    (run! (fn [[_ item]] (free-item item)) free-items)
    (set! free-items {})
    (set! taken-items {})))

(defn create [max-size create-item free-item]
  (let [pool (DynamicResourcePool. {} {} max-size create-item free-item)]
    (pool/add-pool pool)
    pool))
