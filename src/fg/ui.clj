(ns fg.ui
  (:require [minimax.ui.elements :refer [root text rect]]
            [minimax.ui.animation :as ui.anim])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.nanovg NanoVG)
           (org.lwjgl.system MemoryUtil)))

(def ^FloatBuffer buff (MemoryUtil/memAllocFloat 100))

(defn measure-fps [dt]
  (let [pos (int (mod (inc (.position buff)) 100))]
    (.put buff pos (float (/ 1000 dt)))
    (.position buff pos)
    (loop [idx 99
           sum 0]
      (if (>= idx 0)
        (recur (dec idx) (+ sum (.get buff idx)))
        (Math/round (float (/ sum 100)))))))

(defn dialog [props & children]
  (apply rect
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

(defn menu-item [props child-text]
  (rect
    {:style {:padding [4 0 4 0]}}
    (text
      {:font-size 14
       :font-face "RobotoSlab-Bold"
       :text-color #ui/rgba [115 90 25 1]
       :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
      child-text)))

;; TODO: stateful UI components
(def button-state (atom nil))
(def button-spring (ui.anim/make-spring 5 1 2))
(def f (volatile! (constantly 1)))
(def click (atom 0))

(defn button [props child-text]
  (rect
    {:on-mouse-down (fn []
                      (swap! click inc)
                      (if (odd? @click)
                        (vreset! f #(button-spring 1 2 0.16))
                        (vreset! f #(button-spring 2 1 0.16))))
     :on-mouse-up #(prn "mouse up")
     :style
     (merge
       {:padding [8 8 8 8]
        :height (* 32 (@f))
        :background-color #ui/rgba [40 210 40 1]
        :hover/background-color #ui/rgba [50 230 50 1]
        :active/background-color #ui/rgba [20 150 10 1]
        :border-width 2
        :border-color #ui/rgba [30 70 30 1]
        :border-radius 4
        :justify-content :center
        :align-items :center}
       (:style props))}
    (text
      {:font-size 16
       :font-face "RobotoSlab-Bold"
       :text-color #ui/rgba [230 250 230 1]
       :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
      child-text)))

(defn ui-root [dt width height]
  (let [fps (measure-fps dt)]
    (root
      {:width width
       :height height
       :flex-direction :column}
      (dialog
        {:style {:margin [8 8 8 8]
                 :justify-content :center
                 :width 80}}
        (text
          {:font-size 14
           :font-face "RobotoSlab-Bold"
           :text-color #ui/rgba [115 90 25 1]
           :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
          (str "FPS: " fps)))
      (rect
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
      (rect
        {:style {:height 64
                 :justify-content :center
                 :align-items :center}}
        (dialog
          {:style {:padding [8 32 8 32]}}
          (text
            {:font-size 16
             :font-face "RobotoSlab-Bold"
             :text-color #ui/rgba [115 90 25 1]
             :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
            "Welcome!"))))))
