(ns minimax.clock
  (:refer-clojure :exclude [time]))

(set! *warn-on-reflection* true)

(defprotocol IClock
  (step [this])
  (dt [this])
  (time [this]))

(deftype Clock [state dt]
  IClock
  (step [this]
    (let [ct (System/nanoTime)
          _ (vreset! dt (- ct @state))
          _ (vreset! state ct)]
      @dt))
  (dt [this]
    @dt)
  (time [this]
    (System/currentTimeMillis)))

(defn create []
  (Clock. (volatile! (System/nanoTime)) (volatile! 0)))
