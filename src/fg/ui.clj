(ns fg.ui
  (:require [minimax.ui.elements :refer [root text rect]])
  (:import (org.lwjgl.nanovg NanoVG)))

(defn ui-root [dt width height]
  (let [dt (int (/ 1000 dt))]
    (root
      {:width width
       :height height
       :flex-direction :column}
      (rect
        {:width 200
         :height 35
         :margin [8 0 0 8]
         :padding [4 4 4 4]
         :background-color #ui/rgba [36 36 36 1]}
        (text
          {:font-size 16
           :font-face "VarelaRound"
           :text-color #ui/rgba [20 245 55 1]
           :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}
          (str dt "fps"))))))
