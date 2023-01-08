(ns minimax.glfw
  (:import (org.lwjgl.glfw Callbacks GLFW)
           (org.lwjgl.system MemoryUtil)))

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
  (let [x (MemoryUtil/memAllocFloat 1)
        y (MemoryUtil/memAllocFloat 1)
        _ (GLFW/glfwGetMonitorContentScale
            (GLFW/glfwGetPrimaryMonitor) x y)
        dpr (.get x)
        _ (MemoryUtil/memFree x)
        _ (MemoryUtil/memFree y)]
    dpr))
