(ns minimax.glfw
  (:import (org.lwjgl.glfw Callbacks GLFW)))

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

#_(def cursor-arrow (GLFW/glfwCreateStandardCursor GLFW/GLFW_ARROW_CURSOR))
(def cursor-hand (delay (GLFW/glfwCreateStandardCursor GLFW/GLFW_HAND_CURSOR)))

(defn set-cursor* [window cursor]
  (let [cursor (case cursor
                 :default 0
                 :pointer @cursor-hand)]
    (GLFW/glfwSetCursor window cursor)))

(defn set-cursor [cursor]
  (set-cursor* @!window cursor))
