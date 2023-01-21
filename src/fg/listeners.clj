(ns fg.listeners
  (:require
    [bgfx.core :as bgfx]
    [fg.state :as state])
  (:import (org.joml Vector3f)
           (org.lwjgl.bgfx BGFX)
           (org.lwjgl.glfw GLFW GLFWCharCallback GLFWCursorPosCallback GLFWFramebufferSizeCallback GLFWKeyCallback
                           GLFWMouseButtonCallback GLFWScrollCallback GLFWWindowIconifyCallback
                           GLFWWindowMaximizeCallback GLFWWindowSizeCallback)))

(set! *warn-on-reflection* true)

(def texture-format
  BGFX/BGFX_TEXTURE_FORMAT_RGBA8)

(def reset-flags
  (bit-or 0 BGFX/BGFX_RESET_VSYNC))

(state/add-state-listener :mouse-move [:mx :my]
  (fn [{:keys [mouse-button mouse-button-action mx my width at eye]}]
    #_
    (condp = [mouse-button mouse-button-action]
      [GLFW/GLFW_MOUSE_BUTTON_LEFT GLFW/GLFW_PRESS]
      (let [p (- (/ mx width) 0.5)]
        (.setComponent ^Vector3f at 0 p)
        (.setComponent ^Vector3f eye 0 p))
      nil)))

(def key-callback
  (proxy [GLFWKeyCallback] []
    (invoke [window key scancode action mods]
      (let [action (condp = action
                     GLFW/GLFW_PRESS :press
                     GLFW/GLFW_REPEAT :repeat
                     GLFW/GLFW_RELEASE :release)]
        (swap! state/state assoc :key key :key-action action :key-mods mods)
        (when (and (= key GLFW/GLFW_KEY_ESCAPE)
                   (= action :release))
          (GLFW/glfwSetWindowShouldClose window true))))))

(def char-callback
  (proxy [GLFWCharCallback] []
    (invoke [window codepoint]
      (swap! state/state assoc :char-codepoint codepoint))))


(def window-resize-callback
  (proxy [GLFWWindowSizeCallback] []
    (invoke [window w h]
      (swap! state/state assoc :width w :height h))))


(defn create-fb-resize-callback [on-resize]
  (proxy [GLFWFramebufferSizeCallback] []
    (invoke [window width height]
      (on-resize width height))))

(def mouse-move-callback
  (proxy [GLFWCursorPosCallback] []
    (invoke [window x y]
      (swap! state/state assoc :mx x :my y))))

(def mouse-press-callback
  (proxy [GLFWMouseButtonCallback] []
    (invoke [window button action mods]
      (swap! state/state assoc :mouse-button button :mouse-button-action action))))

(def mouse-scroll-callback
  (proxy [GLFWScrollCallback] []
    (invoke [window x y]
      (swap! state/state assoc :sx x :sy y))))

(def minimize-callback
  (proxy [GLFWWindowIconifyCallback] []
    (invoke [window minimized?]
      (swap! state/state assoc :minimized? minimized?))))

(def maximize-callback
  (proxy [GLFWWindowMaximizeCallback] []
    (invoke [window maximized?]
      (swap! state/state assoc :maximized? maximized?))))

(defn set-listeners [window on-resize]
  (let [fb-resize-callback (create-fb-resize-callback on-resize)]
    (GLFW/glfwSetKeyCallback window key-callback)
    (GLFW/glfwSetCharCallback window char-callback)
    (GLFW/glfwSetWindowSizeCallback window window-resize-callback)
    (GLFW/glfwSetFramebufferSizeCallback window fb-resize-callback)
    (GLFW/glfwSetCursorPosCallback window mouse-move-callback)
    (GLFW/glfwSetMouseButtonCallback window mouse-press-callback)
    (GLFW/glfwSetScrollCallback window mouse-scroll-callback)
    (GLFW/glfwSetWindowIconifyCallback window minimize-callback)
    (GLFW/glfwSetWindowMaximizeCallback window maximize-callback)))
