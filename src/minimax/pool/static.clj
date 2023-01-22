(ns minimax.pool.static
  (:require [minimax.pool.core :as pool]))

(set! *warn-on-reflection* true)

(deftype StaticResourcePool
         [^:volatile-mutable taken-items ^:volatile-mutable free-items
          free-item]
  pool/IResourcePool
  (alloc [this]
    (pool/alloc this []))
  (alloc [this args]
    (assert (seq free-items) (str "Resource pool has reached max size of " (count taken-items) " items " (type (first taken-items))))
    (let [item (first free-items)]
      (set! free-items (disj free-items item))
      (set! taken-items (conj taken-items item))
      item))
  (free [this]
    (set! free-items (into free-items taken-items))
    (set! taken-items #{}))
  (destroy [this]
    (run! free-item taken-items)
    (run! free-item free-items)
    (set! free-items #{})
    (set! taken-items #{})))

(defn create [size create-item free-item]
  (let [items (into #{} (repeatedly (dec size) create-item))
        pool (StaticResourcePool. #{} items free-item)]
    (pool/add-pool pool)
    pool))
