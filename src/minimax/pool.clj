(ns minimax.pool
  "Resource pool supports pre-allocation at create time and
  dynamic allocation at alloc time"
  (:require [minimax.logger :as log]))

(defprotocol IResourcePool
  (alloc [this] [this args])
  (free [this])
  (destroy [this]))

(deftype ResourcePool
  [^:volatile-mutable taken-items ^:volatile-mutable free-items
   free-item]
  IResourcePool
  (alloc [this]
    (alloc this []))
  (alloc [this args]
    (assert (seq free-items) (str "Resource pool has reached max size of " (count taken-items) " items"))
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

;; memoizes allocated object on args at alloc time
;; TODO: add object update code path
(deftype DynamicResourcePool
  [^:volatile-mutable taken-items ^:volatile-mutable free-items
   max-size create-item free-item]
  IResourcePool
  (alloc [this]
    (alloc this []))
  (alloc [this args]
    (if (empty? free-items)
      (do
        (assert (>= max-size (count taken-items)) (str "Resource pool has reached max size of " max-size " items"))
        (let [item (apply create-item args)]
          (set! taken-items (assoc taken-items args item))
          item))
      (let [item (get free-items args)]
        (set! free-items (dissoc free-items args))
        (set! taken-items (assoc taken-items args item))
        item)))
  (free [this]
    (set! free-items (into free-items taken-items))
    (set! taken-items {}))
  (destroy [this]
    (run! (fn [[_ item]] (free-item item)) taken-items)
    (run! (fn [[_ item]] (free-item item)) free-items)
    (set! free-items {})
    (set! taken-items {})))

(def ^:private pools (atom #{}))

;; Public API
(defn create [size create-item free-item]
  (let [items (into #{} (repeatedly (dec size) create-item))
        pool (ResourcePool. #{} items free-item)]
    (swap! pools conj pool)
    pool))

(defn create-dynamic [max-size create-item free-item]
  (let [pool (DynamicResourcePool. {} {} max-size create-item free-item)]
    (swap! pools conj pool)
    pool))

(defn destroy-all []
  (log/debug "Destroying resource pools...")
  (time
    (doseq [pool @pools]
      (destroy pool)))
  (reset! pools #{}))
