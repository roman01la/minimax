(ns fg.listeners
  (:require
   [fg.state :as state]
   [minimax.glfw.listeners :as listeners])
  (:import (org.lwjgl.glfw GLFW)))

(set! *warn-on-reflection* true)

(defn key-callback [window key scancode action mods]
  (let [action (condp = action
                 GLFW/GLFW_PRESS :press
                 GLFW/GLFW_REPEAT :repeat
                 GLFW/GLFW_RELEASE :release)]
    (swap! state/state assoc :key key :key-action action :key-mods mods)
    (when (and (= key GLFW/GLFW_KEY_ESCAPE)
               (= action :release))
      (GLFW/glfwSetWindowShouldClose window true))))

(defn char-callback [window codepoint]
  (swap! state/state assoc :char-codepoint codepoint))

(defn window-resize-callback [window w h]
  (swap! state/state assoc :width w :height h))

(defn mouse-move-callback [window x y]
  (swap! state/state assoc :mx x :my y))

(defn mouse-press-callback [window button action mods]
  (swap! state/state assoc :mouse-button button :mouse-button-action action))

(defn mouse-scroll-callback [window x y]
  (swap! state/state assoc :sx x :sy y))

(defn minimize-callback [window minimized?]
  (swap! state/state assoc :minimized? minimized?))

(defn maximize-callback [window maximized?]
  (swap! state/state assoc :maximized? maximized?))

(defn set-listeners [window on-resize]
  (listeners/add-listener :key window key-callback)
  (listeners/add-listener :char window char-callback)
  (listeners/add-listener :window-size window window-resize-callback)
  (listeners/add-listener :frame-buffer-size window on-resize)
  (listeners/add-listener :cursor-position window mouse-move-callback)
  (listeners/add-listener :mouse-button window mouse-press-callback)
  (listeners/add-listener :scroll window mouse-scroll-callback)
  (listeners/add-listener :window-minimize window minimize-callback)
  (listeners/add-listener :window-maximize window maximize-callback))
