(ns fg.clock
  (:require [minimax.clock :as clock]))

(def clock (clock/create))

(defn step []
  (clock/step clock))

(defn time []
  (clock/time clock))

(defn dt []
  (clock/dt clock))
