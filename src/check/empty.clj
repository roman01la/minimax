(ns check.empty
  (:require
  ;;  [bgfx.core :as bgfx]
   [fg.clock :as clock]
   [fg.dev]
   [fg.listeners :as listeners]
   [fg.state :as state]
   [fg.ui.core]
  ;;  [minimax.debug :as debug]
   [minimax.glfw.core :as glfw]
   [minimax.logger :as log]
  ;;  [minimax.passes :as passes]
   [minimax.pool.core :as pool])
  ;;  [minimax.renderer.view :as view])
  (:import
   (java.util.function Consumer)
   (org.lwjgl.bgfx
    BGFX
    BGFXInit
    BGFXPlatform
    BGFXResolution)
   (org.lwjgl.glfw
    GLFW
    GLFWErrorCallback
    GLFWNativeCocoa
    GLFWNativeWin32
    GLFWNativeX11)
   (org.lwjgl.system Configuration MemoryStack Platform))
  (:gen-class))

(set! *warn-on-reflection* true)

(when (= (Platform/get) Platform/MACOSX)
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))

(.set Configuration/DEBUG true)
(.set Configuration/DEBUG_MEMORY_ALLOCATOR true)
(.set Configuration/DEBUG_STACK true)

;; GLFW window

(when-not (GLFW/glfwInit)
  (throw (IllegalStateException. "Unable to initialize GLFW")))

(def error-callback
  (GLFWErrorCallback/createPrint System/err))

;; (GLFW/glfwSetErrorCallback error-callback)
;; GLFWErrorCallback.createThrow () .set ();
(.set (GLFWErrorCallback/createThrow))

(GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API)

;; (GLFW/glfwWindowHint GLFW/GLFW_COCOA_RETINA_FRAMEBUFFER GLFW/GLFW_TRUE)
(when (= (GLFW/glfwGetPlatform) GLFW/GLFW_PLATFORM_COCOA)
  (GLFW/glfwWindowHint GLFW/GLFW_COCOA_RETINA_FRAMEBUFFER GLFW/GLFW_FALSE))

(def window
  (glfw/create-window
   {:width (:width @state/state)
    :height (:height @state/state)
    :title "minimax"}))

(state/set-size (glfw/detect-dpr))

(def reset-flags
  BGFX/BGFX_RESET_VSYNC)

;; Call bgfx::renderFrame before bgfx::init to signal to bgfx not to create a render thread.
;; Most graphics APIs must be used on the same thread that created the window.
(BGFXPlatform/bgfx_render_frame -1)

(with-open [stack (MemoryStack/stackPush)]
  ;; (let [^BGFXInit init (bgfx/create-init)]
  (let [init (BGFXInit/malloc stack)]
    (BGFX/bgfx_init_ctor init)

    (.resolution init
                 (reify Consumer
                   (accept [this it]
                     (-> ^BGFXResolution it
                         (.width (:vwidth @state/state))
                         (.height (:vheight @state/state))
                         (.reset reset-flags)))))

    (condp = (Platform/get)
      Platform/MACOSX
      (-> (.platformData init)
          (.nwh (GLFWNativeCocoa/glfwGetCocoaWindow window)))

      Platform/LINUX
      (-> (.platformData init)
          (.ndt (GLFWNativeX11/glfwGetX11Display))
          (.nwh (GLFWNativeX11/glfwGetX11Window window)))

      Platform/WINDOWS
      (-> (.platformData init)
          (.nwh (GLFWNativeWin32/glfwGetWin32Window window))))

    (when-not (BGFX/bgfx_init init)
      (throw (RuntimeException. "Error initializing bgfx renderer")))))

(log/debug (str "bgfx renderer: " (BGFX/bgfx_get_renderer_name (BGFX/bgfx_get_renderer_type))))

;; (ui/init)
;; (audio/init)

;; debug
;; (def selected-object (atom nil))
;; (def debug-box @debug/debug-box)

;; Rendering loop
(def curr-frame (volatile! 0))

(defn run []
  (let [dt (clock/dt)
        t (clock/time)]

    ;; (println "run frame" @curr-frame)
    (let [v-width (:vwidth @state/state)
          v-height (:vheight @state/state)]
      ;; (view/rect passes/geometry 0 0 v-width v-height)
      ;; (view/clear passes/geometry
      ;;             (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH)
      ;;             (:background-color @state/state)))

      ;; (view/touch passes/geometry)))
      (BGFX/bgfx_set_view_rect 0 0 0 v-width v-height)
      (BGFX/bgfx_set_view_clear 0 (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH) 
                                ;; 0x303030ff
                                0x37eb34ff
                                1.0
                                0)
      (BGFX/bgfx_touch 0))))

(def fb-size (volatile! nil))

(defn on-resize [width height]
  (swap! state/state assoc :vwidth width :vheight height)
  ;; (swap! camera assoc :aspect (/ width height))
  (BGFX/bgfx_reset width height reset-flags BGFX/BGFX_TEXTURE_FORMAT_COUNT))

;; (listeners/set-listeners window
;;                          (fn [_ width height]
;;                            (vreset! fb-size [width height])))

;; resize to the latest size value in a rendering loop
(defn maybe-set-size []
  (when (some? @fb-size)
    (let [[fbw fbh] @fb-size
          {:keys [vwidth vheight]} @state/state]
      (when (or (not= fbw vwidth)
                (not= fbh vheight))
        (on-resize fbw fbh)))))

(defn -main [& args]
  ;; start file watcher
  ;; (fg.dev/start)

  ;; TODO: Add sound control UI
  ;; #_(audio/play :bg)

  (while (not (GLFW/glfwWindowShouldClose window))
    ;; (println "main frame" @curr-frame)
    (state/reset-state)
    (GLFW/glfwPollEvents)
    ;; (maybe-set-size)
    (clock/step)
    (run)
    ;(vreset! curr-frame (bgfx/frame)))
    (vswap! curr-frame inc)
    (BGFX/bgfx_frame false))

  ;; Disposing the program
  (pool/destroy-all)
  ;; (ui/shutdown)
  ;; (audio/shutdown)
  (BGFX/bgfx_shutdown)
  (glfw/destroy-window)
  (GLFW/glfwTerminate)
  (.free (GLFW/glfwSetErrorCallback nil)))
  ;; Stop file watcher
  ;; (fg.dev/stop))
