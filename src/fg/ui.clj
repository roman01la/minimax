(ns fg.ui
  (:require [minimax.ui.elements :refer [root text rect]])
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

(defn ui-root [dt width height]
  (let [fps (measure-fps dt)]
    (root
      {:width width
       :height height
       :flex-direction :column}
      (rect
        {:margin [8 8 8 8]
         :padding [8 8 8 8]
         :justify-content :center
         :width 80
         :background-color #ui/rgba [230 190 80 1]
         :border-width 4
         :border-color #ui/rgba [210 160 35 1]
         :border-radius 4}
        (text
          {:font-size 14
           :font-face "RobotoSlab-Bold"
           :text-color #ui/rgba [115 90 25 1]
           :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
          (str "FPS: " fps)))
      (rect
        {:flex 1})
      (rect
        {:height 64
         :justify-content :center
         :align-items :center}
        (rect
          {:padding [8 32 8 32]
           :border-radius 4
           :background-color #ui/rgba [230 190 80 1]
           :border-width 4
           :border-color #ui/rgba [210 160 35 1]}
          (text
            {:font-size 16
             :font-face "RobotoSlab-Bold"
             :text-color #ui/rgba [115 90 25 1]
             :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
            "Welcome!"))))))
