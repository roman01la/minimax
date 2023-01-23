(ns minimax.glfw.core
  (:require [minimax.mem :as mem])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.glfw Callbacks GLFW)))

(set! *warn-on-reflection* true)

(def !window (atom nil))

(defn create-window [{:keys [width height title]}]
  (let [window (GLFW/glfwCreateWindow ^int width ^int height ^CharSequence title 0 0)]
    (when (nil? window)
      (throw (RuntimeException. "Failed to create the GLFW window")))
    (reset! !window window)
    window))

(defn destroy-window []
  (Callbacks/glfwFreeCallbacks @!window)
  (GLFW/glfwDestroyWindow @!window))

(def cursor-hand (delay (GLFW/glfwCreateStandardCursor GLFW/GLFW_HAND_CURSOR)))

(defn set-cursor* [window cursor]
  (let [cursor (case cursor
                 :default 0
                 :pointer @cursor-hand)]
    (GLFW/glfwSetCursor window cursor)))

(defn set-cursor [cursor]
  (set-cursor* @!window cursor))

(defn detect-dpr []
  (mem/slet [^FloatBuffer x [:float 1]
             ^FloatBuffer y [:float 1]]
    (GLFW/glfwGetMonitorContentScale
     (GLFW/glfwGetPrimaryMonitor) x y)
    (.get x)))
