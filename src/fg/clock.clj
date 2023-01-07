(ns fg.clock
  (:refer-clojure :exclude [time])
  (:require [minimax.clock :as clock]))

(def clock (clock/create))

(defn step []
  (clock/step clock))

(defn time []
  (clock/time clock))

(defn dt []
  (clock/dt clock))
