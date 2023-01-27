(ns fg.ui.debug
  (:require
   [bgfx.core :as bgfx]
   [clojure.string :as str]
   [minimax.mem :as mem]
   [minimax.object :as obj]
   [minimax.objects.group]
   [minimax.objects.light]
   [minimax.objects.mesh]
   [minimax.objects.scene]
   [minimax.renderer.ui]
   [minimax.ui.animation :as ui.anim]
   [minimax.ui.components :as mui :refer [defui]]
   [minimax.ui.diff :as diff :refer [$]]
   [minimax.ui.elements :as ui])
  (:import (java.nio FloatBuffer)
           (minimax.objects.group Group)
           (minimax.objects.light DirectionalLight)
           (minimax.objects.mesh Mesh)
           (minimax.objects.scene Scene)
           (org.lwjgl.nanovg NanoVG)))

(def ^FloatBuffer buff (mem/alloc :float 100))

(defn measure-fps [dt]
  (let [pos (int (mod (inc (.position buff)) 100))]
    (.put buff pos (float (/ 1000 dt)))
    (.position buff pos)
    (loop [idx 99
           sum 0]
      (if (>= idx 0)
        (recur (dec idx) (+ sum (.get buff idx)))
        (Math/round (double (/ sum 100)))))))

(def text-styles
  {:font-size 10
   :font-face "IBMPlexMono-Regular"
   :text-color #ui/rgba [230 230 230 1]
   :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)})

(defn stats-text [{:keys [children]}]
  ($ :text
     {:style (merge text-styles {:margin [4 0]})}
     children))

(defn perf-stats [_]
  (let [stats (bgfx/get-stats)
        to-ms-cpu (double (/ 1000 (.cpuTimerFreq stats)))
        to-ms-gpu (double (/ 1000 (.gpuTimerFreq stats)))
        frame-ms (measure-fps (* (.cpuTimeFrame stats) to-ms-cpu))
        cpu-submit (double (* to-ms-cpu (- (.cpuTimeEnd stats) (.cpuTimeBegin stats))))
        gpu-submit (double (* to-ms-gpu (- (.gpuTimeEnd stats) (.gpuTimeBegin stats))))
        max-gpu-latency (.maxGpuLatency stats)
        gpu-mem-used (.gpuMemoryUsed stats)
        num-draw-calls (.numDraw stats)]
    ($ mui/widget
       {:title "Performance Monitor"
        :style {:justify-content :center
                :width 240
                :margin [0 0 8 0]}}
       ($ stats-text
          (format "FPS: %d" frame-ms))
       ($ stats-text
          (format "Submit CPU %.1f, GPU %.1f (L: %d)" cpu-submit gpu-submit max-gpu-latency))
       (when (not= (- Long/MAX_VALUE) gpu-mem-used)
         ($ stats-text
            (format "GPU mem: %d" gpu-mem-used)))
       ($ stats-text
          (format "Draw calls: %d" num-draw-calls))
       ($ stats-text
          (format "UI Layout: %.2fms" (double (/ @minimax.renderer.ui/layout-time 1e6)))))))

(defn tree-view-item [{:keys [on-select selected? object]}]
  (mui/button-text
   {:on-mouse-down #(on-select (when-not selected? object))
    :style {:border-radius 2
            :padding [4 8]
            :margin [2 12 2 0]
            :background-color (when selected? #ui/rgba [120 120 255 1])
            :hover/background-color #ui/rgba [80 80 80 1]
            :justify-content :center
            :cursor :pointer}
    :text/style (merge text-styles
                       {:hover/text-color (when-not selected? #ui/rgba [120 120 255 1])})}
   (condp = (type object)
     Scene "<Scene>"
     Mesh (str (:name object) " <Mesh>")
     Group (str (:name object) " <Group>")
     DirectionalLight (str (:name object) " <DirectionalLight>")
     (str (type object)))))

(defn tree-view [{:keys [style on-select object selected]}]
  (let [selected? (= selected object)]
    (ui/view {:style style}
             (tree-view-item
              {:on-select on-select
               :selected? selected?
               :object object})
             (for [obj (:children object)
                   :when (not (:debug-skip? obj))]
               (tree-view
                {:style {:padding [0 0 0 8]}
                 :on-select on-select
                 :object obj
                 :selected selected})))))

(defn search-results [{:keys [scene on-select search-query selected]}]
  (->> (obj/scene->seq scene)
       (filter #(str/starts-with? (:name %) search-query))
       (map #(tree-view-item
              {:on-select on-select
               :selected? (= selected %)
               :object %}))))

(defui scene-graph [scene selected]
  (mui/hlet [expanded? (mui/use-state false)
             search-query (mui/use-state "")
             spring-f (mui/use-state (fn [] (constantly 0)))
             spring (mui/use-memo #(ui.anim/make-spring 5 1 3) [])]
    (let [on-select #(reset! selected %)]
      (mui/scroll-widget
       {:title "Scene Inspector"
        :width 240
        :height #ui/% (* 60 (@spring-f))
        :expanded? @expanded?
        :on-header-click (fn []
                           (if-not @expanded?
                             (reset! spring-f #(spring 0 1 0.16))
                             (reset! spring-f #(spring 1 0 0.16)))
                           (swap! expanded? not))
        :header [(mui/text-input
                  {:style (merge text-styles
                                 {:padding 8
                                  :placeholder-color #ui/rgba [255 255 255 0.7]
                                  :background-color #ui/rgba [60 60 60 1]
                                  :width #ui/% 100})
                   :on-change #(reset! search-query %)
                   :placeholder "Search"
                   :value @search-query})]}
       (if (not= "" @search-query)
         (search-results
          {:scene scene
           :on-select on-select
           :search-query @search-query
           :selected @selected})
         (tree-view
          {:style {:padding [8 0]}
           :object scene
           :on-select on-select
           :selected @selected}))))))

(defn root [{:keys [width height scene selected]}]
  ($ :view
     {:style {:flex 1
              :width width
              :height height
              :flex-direction :column
              :padding 16}}
     ($ perf-stats)
     #_(scene-graph scene selected)))
