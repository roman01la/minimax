(ns minimax.glfw.listeners
  (:import (org.lwjgl.glfw GLFW GLFWCharCallback GLFWCursorPosCallback GLFWFramebufferSizeCallback GLFWKeyCallback
                           GLFWMouseButtonCallback GLFWScrollCallback GLFWWindowIconifyCallback
                           GLFWWindowMaximizeCallback GLFWWindowSizeCallback)))

(defn create-key-callback [f]
  (proxy [GLFWKeyCallback] []
    (invoke [window key scancode action mods]
      (f window key scancode action mods))))

(defn create-char-callback [f]
  (proxy [GLFWCharCallback] []
    (invoke [window codepoint]
      (f window codepoint))))

(defn create-window-resize-callback [f]
  (proxy [GLFWWindowSizeCallback] []
    (invoke [window w h]
      (f window w h))))

(defn create-fb-resize-callback [f]
  (proxy [GLFWFramebufferSizeCallback] []
    (invoke [window width height]
      (f window width height))))

(defn create-mouse-move-callback [f]
  (proxy [GLFWCursorPosCallback] []
    (invoke [window x y]
      (f window x y))))

(defn create-mouse-press-callback [f]
  (proxy [GLFWMouseButtonCallback] []
    (invoke [window button action mods]
      (f window button action mods))))

(defn create-mouse-scroll-callback [f]
  (proxy [GLFWScrollCallback] []
    (invoke [window x y]
      (f window x y))))

(defn create-minimize-callback [f]
  (proxy [GLFWWindowIconifyCallback] []
    (invoke [window minimized?]
      (f window minimized?))))

(defn create-maximize-callback [f]
  (proxy [GLFWWindowMaximizeCallback] []
    (invoke [window maximized?]
      (f window maximized?))))

(defmulti add-listener (fn [type window f] type))

(defmethod add-listener :key [_ window f]
  (GLFW/glfwSetKeyCallback window (create-key-callback f)))

(defmethod add-listener :char [_ window f]
  (GLFW/glfwSetCharCallback window (create-char-callback f)))

(defmethod add-listener :window-size [_ window f]
  (GLFW/glfwSetWindowSizeCallback window (create-window-resize-callback f)))

(defmethod add-listener :frame-buffer-size [_ window f]
  (GLFW/glfwSetFramebufferSizeCallback window (create-fb-resize-callback f)))

(defmethod add-listener :cursor-position [_ window f]
  (GLFW/glfwSetCursorPosCallback window (create-mouse-move-callback f)))

(defmethod add-listener :mouse-button [_ window f]
  (GLFW/glfwSetMouseButtonCallback window (create-mouse-press-callback f)))

(defmethod add-listener :scroll [_ window f]
  (GLFW/glfwSetScrollCallback window (create-mouse-scroll-callback f)))

(defmethod add-listener :window-minimize [_ window f]
  (GLFW/glfwSetWindowIconifyCallback window (create-minimize-callback f)))

(defmethod add-listener :window-maximize [_ window f]
  (GLFW/glfwSetWindowMaximizeCallback window (create-maximize-callback f)))
