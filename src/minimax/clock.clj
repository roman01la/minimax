(ns minimax.clock
  (:refer-clojure :exclude [time])
  (:import (org.lwjgl.glfw GLFW)))

(set! *warn-on-reflection* true)

(defprotocol IClock
  (step [this])
  (dt [this])
  (time [this]))

(deftype Clock [state dt]
  IClock
  (step [this]
    (let [ct (GLFW/glfwGetTime)
          _ (vreset! dt (- ct @state))
          _ (vreset! state ct)]
      @dt))
  (dt [this]
    @dt)
  (time [this]
    (GLFW/glfwGetTime)))

(defn create []
  (Clock. (volatile! 0) (volatile! 0.016)))
