(ns minimax.ui.utils
  (:require [minimax.mem :as mem])
  (:import (java.io File)
           (java.nio IntBuffer)
           (org.lwjgl.nanovg NVGColor NanoVG)
           (org.lwjgl.system MemoryUtil)))

(set! *warn-on-reflection* true)

(defn rgba ^NVGColor [r g b a ^NVGColor color]
  (.r color (/ r 255))
  (.g color (/ g 255))
  (.b color (/ b 255))
  (.a color a)
  color)

(defn point-in-rect? [mx my x y w h]
  (and (>= mx x)
       (>= my y)
       (<= mx (+ x w))
       (<= my (+ y h))))

(defn rects-intersect? [[ax ay aw ah] [bx by bw bh]]
  (let [ax1 ax
        ax2 (+ ax aw)
        ay1 ay
        ay2 (+ ay ah)

        bx1 bx
        bx2 (+ bx bw)
        by1 by
        by2 (+ by bh)]

    (and (or (>= bx2 ax2 bx1)
             (>= bx2 ax1 bx1))
         (or (>= by2 ay2 by1)
             (>= by2 ay1 by1)))))

;; TODO: cache and delete image (lifecycle)
(defn create-image-from-file [^long vg ^File file]
  (mem/slet [^IntBuffer w [:int 1]
             ^IntBuffer h [:int 1]]
    (let [img (NanoVG/nvgCreateImage vg (.getAbsolutePath file) 0)]
      (NanoVG/nvgImageSize vg img w h)
      {:handle img
       :width (.get w)
       :height (.get h)})))
