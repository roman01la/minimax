(ns check.empty2
  (:require
   [bgfx.core :as bgfx]
   [fg.clock :as clock]
   [fg.dev]
   ;[fg.listeners :as listeners]
   ;[fg.model :as md]
   ;[fg.passes.combine :as pass.comb]
   [fg.passes.geometry :as pass.geom]
   ;[fg.passes.shadow :as pass.shadow]
   [fg.state :as state]
  ;;  [fg.ui.core]
  ;;  [minimax.audio.core :as audio]
   ;[minimax.debug :as debug]
   [minimax.glfw.core :as glfw]
   [minimax.logger :as log]
   ;[minimax.object :as obj]
   [minimax.objects.camera :as camera]
   ;[minimax.objects.light :as light]
   ;[minimax.objects.scene :as scene]
   [minimax.passes :as passes]
   [minimax.pool.core :as pool]
   [minimax.renderer.view :as view]
   ;[minimax.renderer.ui :as ui]
   )
  (:import
   (java.util.function Consumer)
   (org.joml Matrix4f Vector3f)
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
   (org.lwjgl.system Configuration Platform))
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

(GLFW/glfwSetErrorCallback error-callback)
;(.set (GLFWErrorCallback/createThrow))

(GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API)

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

(let [^BGFXInit init (bgfx/create-init)]
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

  (when-not (bgfx/init init)
    (throw (RuntimeException. "Error initializing bgfx renderer"))))

(log/debug (str "bgfx renderer: " (bgfx/get-renderer-name (bgfx/get-renderer-type))))

;; (ui/init)
;; (audio/init)

(def camera
  (atom
   (camera/create-perspective-camera
    {:fov 60
     :aspect (/ (:vwidth @state/state) (:vheight @state/state))
     :near 0.1
     :far 100})))

;; (def d-light
;;   (light/create-directional-light
;;    {:name "u_light_pos"
;;     :position (:light-pos @state/state)
;;     :color (Vector3f. 1.0 1.0 1.0)}))

;; (def model
;;   (do
;;     (log/debug "Importing model...")
;;     (log/time (md/load-model "models/castle.glb"))))

;; debug
;; (def selected-object (atom nil))
;; (def debug-box @debug/debug-box)

;; scene
;; (def scene
;;   (atom (scene/create
;;          {:name "MainScene"
;;           :children [d-light (:scene model) debug-box]})))

;; (def castle-obj
;;   (obj/find-by-name @scene "castle_root"))

;; (def cloud-1-obj
;;   (obj/find-by-name @scene "cloud_1"))

;; (def cloud-2-obj
;;   (obj/find-by-name @scene "cloud_2"))

(defn render [dt t]
  (let [t (* t 10)
        ;; pos1 (obj/position cloud-1-obj)
        ;; pos2 (obj/position cloud-2-obj)
        y (-> (Math/sin (/ t 2))
              (/ 100))
        x (-> (Math/sin (/ t 10))
              (/ 100))
        z (-> (Math/cos (/ t 10))
              (/ 100))
        v-width (:vwidth @state/state)
        v-height (:vheight @state/state)]

    ;; (obj/rotate-y cloud-1-obj (* dt 0.3))
    ;; (obj/set-position-y cloud-1-obj (+ (.y pos1) y))
    ;; (obj/set-position-x cloud-1-obj (+ (.x pos1) x))
    ;; (obj/set-position-z cloud-1-obj (+ (.z pos1) z))

    ;; (obj/rotate-y cloud-2-obj (* dt -0.3))
    ;; (obj/set-position-y cloud-2-obj (+ (.y pos2) y))
    ;; (obj/set-position-x cloud-2-obj (+ (.x pos2) z))
    ;; (obj/set-position-z cloud-2-obj (+ (.z pos2) x))

    ;; (obj/rotate-y castle-obj (* dt 0.1))

    ;; (vreset! (:visible? debug-box) (some? @selected-object))

    ;; (when-let [obj @selected-object]
    ;;   (let [[root & objs] (reverse (obj/obj->parent-seq obj []))
    ;;         mtx (->> objs
    ;;                  (reduce
    ;;                   (fn [mtx obj]
    ;;                     (obj/apply-matrix* mtx (:lmtx obj) mtx))
    ;;                   (.set (Matrix4f.) ^Matrix4f (:mtx root))))]
    ;;     (debug/set-object-transform obj debug-box mtx)))))
    ;;  (BGFX/bgfx_touch)

    ;;  (view/rect passes/geometry 0 0 v-width v-height)
    ;;  (view/clear passes/geometry
    ;;              (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH)
    ;;              0x37eb34ff 1.0)
    ;;             ;;  (:background-color @state/state))
     
    ;;  (view/touch passes/geometry)

     (view/rect passes/shadow 0 0 v-width v-height)
     (view/clear passes/shadow
                 (bit-or BGFX/BGFX_CLEAR_COLOR BGFX/BGFX_CLEAR_DEPTH)
                 0x37eb34ff 1.0)
                ; (:background-color @state/state))
     
     (view/touch passes/shadow)))

;; (defn render-ui []
;;   (fg.ui.core/test-root @state/state @scene selected-object))

;; Rendering loop
(def curr-frame (volatile! 0))

(defn run []
  (let [dt (clock/dt)
        t (clock/time)]

    #_(pass.shadow/setup d-light) ;; setup pass shadow
    (pass.geom/setup camera) ;; render pass geometry

    (render dt t)

    #_(obj/render @scene (:id passes/shadow)) ;; fill shadow map texture
    #_(obj/render @scene (:id passes/geometry)) ;; fill screen space texture

    #_(ui/render @state/state render-ui) ;; ui pass

    #_(pass.comb/render))) ;; render combine pass

;; (def fb-size (volatile! nil))

;; (defn on-resize [width height]
;;   (swap! state/state assoc :vwidth width :vheight height)
;;   (swap! camera assoc :aspect (/ width height))
;;   (bgfx/reset width height reset-flags))

;; (listeners/set-listeners window
;;                          (fn [_ width height]
;;                            (vreset! fb-size [width height])))

;; resize to the latest size value in a rendering loop
;; (defn maybe-set-size []
;;   (when (some? @fb-size)
;;     (let [[fbw fbh] @fb-size
;;           {:keys [vwidth vheight]} @state/state]
;;       (when (or (not= fbw vwidth)
;;                 (not= fbh vheight))
;;         (on-resize fbw fbh)))))

(defn -main [& args]
  #_(fg.dev/start)

  ;; TODO: Add sound control UI
  #_(audio/play :bg)

  (while (not (GLFW/glfwWindowShouldClose window))
    #_(println "Frame begin: " @curr-frame)
    (state/reset-state)
    (GLFW/glfwPollEvents)
    #_(maybe-set-size)
    (clock/step)
    (run)
    #_(vreset! curr-frame (bgfx/frame))
    (vswap! curr-frame inc)
    (BGFX/bgfx_frame false) 
    #_(println "Frame end: " @curr-frame))

  ;; Disposing the program
  (pool/destroy-all)
  #_(ui/shutdown)
  #_(audio/shutdown)
  (bgfx/shutdown)
  (glfw/destroy-window)
  (GLFW/glfwTerminate)
  (.free (GLFW/glfwSetErrorCallback nil)
  ;; Stop file watcher
  #_(fg.dev/stop)))
