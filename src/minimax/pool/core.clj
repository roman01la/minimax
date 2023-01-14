(ns minimax.pool.core
  (:require [minimax.logger :as log]))

(defprotocol IResourcePool
  (alloc [this] [this args])
  (free [this])
  (destroy [this]))

(def ^:private pools (atom #{}))

(defn add-pool [pool]
  (swap! pools conj pool))

(defn destroy-all []
  (log/debug "Destroying resource pools...")
  (time
    (doseq [pool @pools]
      (destroy pool)))
  (reset! pools #{}))
