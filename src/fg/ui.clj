(ns fg.ui
  (:require
    [minimax.ui.elements :as ui]
    [minimax.ui.animation :as ui.anim]
    [minimax.ui.components :as mui :refer [defui]])
  (:import (java.nio FloatBuffer)
           (minimax.objects.group Group)
           (minimax.objects.light DirectionalLight)
           (minimax.objects.mesh Mesh)
           (minimax.objects.scene Scene)
           (org.lwjgl.nanovg NanoVG)
           (org.lwjgl.system MemoryUtil)))

(def ^FloatBuffer buff (MemoryUtil/memAllocFloat 100))

(defn measure-fps [dt]
  (let [pos (int (mod (inc (.position buff)) 100))]
    (.put buff pos (float (/ 1 dt)))
    (.position buff pos)
    (loop [idx 99
           sum 0]
      (if (>= idx 0)
        (recur (dec idx) (+ sum (.get buff idx)))
        (Math/round (float (/ sum 100)))))))

(defui dialog [props & children]
  (apply ui/view
    (merge
      props
      {:style (merge
                {:padding [8 8 8 8]
                 :background-color #ui/rgba [230 190 80 1]
                 :border-width 2
                 :border-color #ui/rgba [210 160 35 1]
                 :border-radius 4}
                (:style props))})
    children))

(defui menu-item [props child-text]
  (ui/view
    {:style {:padding [4 0 4 0]}}
    (ui/text
      {:style {:font-size 14
               :font-face "RobotoSlab-Bold"
               :text-color #ui/rgba [115 90 25 1]
               :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
      child-text)))

;; TODO: stateful UI components
(def button-state (atom nil))
(def button-spring (ui.anim/make-spring 5 1 2))
(def f (volatile! (constantly 1)))
(def click (atom 0))

(defui button [props child-text]
  (let [state (mui/use-state {:hover? false :pressed? false})]
    (mui/view
      {:on-mouse-down (fn []
                        (swap! state assoc :pressed? true)

                        (swap! click inc)
                        (if (odd? @click)
                          (vreset! f #(button-spring 1 2 0.16))
                          (vreset! f #(button-spring 2 1 0.16))))
       :on-mouse-up #(swap! state assoc :pressed? false)
       :on-mouse-enter #(swap! state assoc :hover? true)
       :on-mouse-leave #(swap! state assoc :hover? false)
       :style
       (merge
         {:padding [8 8 8 8]
          :height (* 32 (@f))
          :background-color (cond
                              (:pressed? @state) #ui/rgba [20 150 10 1]
                              (:hover? @state) #ui/rgba [50 230 50 1]
                              :else #ui/rgba [40 210 40 1])
          :border-width 2
          :border-color #ui/rgba [30 70 30 1]
          :border-radius 4
          :justify-content :center
          :align-items :center}
         (:style props)
         {:margin (if (:pressed? @state)
                    [40 0 0 0]
                    (:margin (:style props)))})}
      (ui/text
        {:style {:font-size 16
                 :font-face "RobotoSlab-Bold"
                 :text-color #ui/rgba [230 250 230 1]
                 :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
        child-text))))

(defui game-ui [dt width height]
  (let [fps (measure-fps dt)]
    (ui/root
      {:style {:width width
               :height height
               :flex-direction :column}}
      (dialog
        {:style {:margin [8 8 8 8]
                 :justify-content :center
                 :width 80}}
        (ui/text
          {:style {:font-size 14
                   :font-face "RobotoSlab-Bold"
                   :text-color #ui/rgba [115 90 25 1]
                   :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
          (str "FPS: " fps)))
      (ui/view
        {:style {:flex 1
                 :justify-content :center}}
        (dialog
          {:style {:width 180
                   :margin [0 16 0 0]
                   :align-self :flex-end}}
          (menu-item {} "Buildings: 1")
          (menu-item {} "Fruit trees: 2")
          (button {:style {:margin [32 0 0 0]}}
            "Add item")))
      (ui/view
        {:style {:height 64
                 :justify-content :center
                 :align-items :center}}
        (dialog
          {:style {:padding [8 32 8 32]}}
          (ui/text
            {:style {:font-size 16
                     :font-face "RobotoSlab-Bold"
                     :text-color #ui/rgba [115 90 25 1]
                     :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
            "Welcome!"))))))

(defn tree-view [{:keys [style on-select object selected]}]
  (let [selected? (= selected object)]
    (ui/view {:style style}
      (ui/view
        {}
        (mui/button-text
          {:on-mouse-down #(on-select (if selected? nil object))
           :style {:border-radius 2
                   :padding [4 8]
                   :margin [2 12 2 0]
                   :background-color (when selected? #ui/rgba [120 120 255 1])
                   :hover/background-color #ui/rgba [80 80 80 1]
                   :justify-content :center
                   :cursor :pointer}
           :text/style {:font-size 10
                        :font-face "IBMPlexMono-Regular"
                        :text-color #ui/rgba [230 230 230 1]
                        :hover/text-color (when-not selected? #ui/rgba [120 120 255 1])
                        :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
          (condp = (type object)
            Scene "<Scene>"
            Mesh (str (:name object) " <Mesh>")
            Group (str (:name object) " <Group>")
            DirectionalLight (str (:name object) " <DirectionalLight>")
            (type object))))
      (for [obj (:children object)
            :when (not (:debug-skip? obj))]
        (tree-view
          {:style {:padding [0 0 0 8]}
           :on-select on-select
           :object obj
           :selected selected})))))

(def widget-spring (ui.anim/make-spring 5 1 3))
(def widget-spring-f (volatile! (constantly 0)))

(defui scene-graph [scene selected]
  (let [expanded? (mui/use-state false)]
    (mui/widget
      {:width 240
       :height (* 500 (@widget-spring-f))
       :expanded? @expanded?
       :on-header-click (fn []
                          (if-not @expanded?
                            (vreset! widget-spring-f #(widget-spring 0 1 0.16))
                            (vreset! widget-spring-f #(widget-spring 1 0 0.16)))
                          (swap! expanded? not))}
      (tree-view
        {:style {:padding [8 0]}
         :object scene
         :on-select #(reset! selected %)
         :selected @selected}))))

(defn debug-ui [width height scene selected]
  (ui/root
    {:style {:width width
             :height height
             :flex-direction :column
             :padding 8}}
    (scene-graph scene selected)))

(defui ui-root [dt width height scene selected]
  #_(game-ui dt width height)
  (debug-ui width height scene selected))

