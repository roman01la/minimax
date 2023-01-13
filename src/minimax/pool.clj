(ns minimax.pool)

(defprotocol IResourcePool
  (alloc [this])
  (free [this] [this item]))

(deftype ResourcePool [max-size ^:volatile-mutable taken-items ^:volatile-mutable free-items create-item free-item]
  IResourcePool
  (alloc [this]
    (when (empty? free-items)
      (assert (<= (count taken-items) max-size) (str "Resource pool has reached max size of " max-size " items"))
      (set! free-items (conj free-items (create-item))))
    (let [item (first free-items)]
      (set! free-items (disj free-items item))
      (set! taken-items (conj taken-items item))
      item))
  (free [this]
    (run! free-item taken-items)
    (run! free-item free-items)
    (set! free-items #{})
    (set! taken-items #{}))
  (free [this item]
    (free-item item)
    (set! taken-items (disj taken-items item))
    (set! free-items (conj free-items (create-item)))))

(defn create [max-size create-item free-item]
  (ResourcePool. max-size #{} #{} create-item free-item))
