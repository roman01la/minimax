(ns minimax.ui.primitives
  (:require
   [clojure.java.io :as io]
   [minimax.mem :as mem]
   [minimax.pool.core :as pool]
   [minimax.resources :as res]
   [minimax.ui.context :as ui.ctx]
   [minimax.ui.utils :as ui.utils])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.nanovg NVGColor NVGPaint NanoVG)
           (org.lwjgl.util.yoga YGNode Yoga)))

(set! *warn-on-reflection* true)

(defprotocol IDrawable
  (draw [this]))

(defprotocol ILayout
  (layout [this])
  (store-layout [this])
  (get-layout [this]))

(def style-prop-mapping
  {:flex-direction {:column Yoga/YGFlexDirectionColumn
                    :row Yoga/YGFlexDirectionRow}
   :justify-content {:center Yoga/YGJustifyCenter
                     :flex-end Yoga/YGJustifyFlexEnd
                     :flex-start Yoga/YGJustifyFlexStart
                     :space-around Yoga/YGJustifySpaceAround
                     :space-between Yoga/YGJustifySpaceBetween
                     :space-evenly Yoga/YGJustifySpaceEvenly}
   :align-items {:center Yoga/YGAlignCenter
                 :flex-end Yoga/YGAlignFlexEnd
                 :flex-start Yoga/YGAlignFlexStart
                 :space-around Yoga/YGAlignSpaceAround
                 :space-between Yoga/YGAlignSpaceBetween}
   :align-self {:center Yoga/YGAlignCenter
                :stretch Yoga/YGAlignStretch
                :flex-end Yoga/YGAlignFlexEnd
                :flex-start Yoga/YGAlignFlexStart}
   :display {:flex Yoga/YGDisplayFlex}
   :position {:static Yoga/YGPositionTypeStatic
              :relative Yoga/YGPositionTypeRelative
              :absolute Yoga/YGPositionTypeAbsolute}})

(defn prop-shorthand [f v]
  (cond
    (number? v) (f Yoga/YGEdgeAll v)
    (= 4 (count v)) (let [[top right bottom left] v]
                      (f Yoga/YGEdgeTop top)
                      (f Yoga/YGEdgeRight right)
                      (f Yoga/YGEdgeBottom bottom)
                      (f Yoga/YGEdgeLeft left))
    (= 3 (count v)) (let [[top h bottom] v]
                      (f Yoga/YGEdgeTop top)
                      (f Yoga/YGEdgeHorizontal h)
                      (f Yoga/YGEdgeBottom bottom))
    (= 2 (count v)) (let [[v h] v]
                      (f Yoga/YGEdgeHorizontal h)
                      (f Yoga/YGEdgeVertical v))))

(defrecord PercentValue [value])

(def styles-mapping
  {:flex-direction #(Yoga/YGNodeStyleSetDirection %1 (get-in style-prop-mapping [:flex-direction %2]))
   :justify-content #(Yoga/YGNodeStyleSetJustifyContent %1 (get-in style-prop-mapping [:justify-content %2]))
   :align-items #(Yoga/YGNodeStyleSetAlignItems %1 (get-in style-prop-mapping [:align-items %2]))
   :align-self #(Yoga/YGNodeStyleSetAlignSelf %1 (get-in style-prop-mapping [:align-self %2]))

   :margin (fn [node v]
             (prop-shorthand #(Yoga/YGNodeStyleSetMargin node %1 %2) v))
   :padding (fn [node v]
              (prop-shorthand #(Yoga/YGNodeStyleSetPadding node %1 %2) v))

   :display #(Yoga/YGNodeStyleSetDisplay %1 (get-in style-prop-mapping [:display %2]))
   :position #(Yoga/YGNodeStyleSetPositionType %1 (get-in style-prop-mapping [:position %2]))
   :flex #(Yoga/YGNodeStyleSetFlex %1 %2)

   :height (fn [node v]
             (condp = (type v)
               PercentValue (Yoga/YGNodeStyleSetHeightPercent node (:value v))
               (Yoga/YGNodeStyleSetHeight node v)))
   :width (fn [node v]
            (condp = (type v)
              PercentValue (Yoga/YGNodeStyleSetWidthPercent node (:value v))
              (Yoga/YGNodeStyleSetWidth node v)))

   :top #(Yoga/YGNodeStyleSetPosition %1 Yoga/YGEdgeTop %2)
   :left #(Yoga/YGNodeStyleSetPosition %1 Yoga/YGEdgeLeft %2)
   :right #(Yoga/YGNodeStyleSetPosition %1 Yoga/YGEdgeRight %2)
   :bottom #(Yoga/YGNodeStyleSetPosition %1 Yoga/YGEdgeBottom %2)})

(defn create-node [styles]
  (let [node (Yoga/YGNodeNew)]
    (doseq [[k v] styles]
      (when-let [f (styles-mapping k)]
        (f node v)))
    node))

(defn measure-text [{:keys [font-size font-face text-align]} text]
  (assert (number? font-size) "font-size should be a number")
  (assert (string? font-face) "font-face should be a string")
  (assert (int? text-align) "text-align should be an int")

  (mem/slet [^FloatBuffer text-bounds [:float 4]]

            (let [^long vg @ui.ctx/vg]
              (NanoVG/nvgFontSize vg font-size)
              (NanoVG/nvgFontFace vg ^CharSequence font-face)
              (NanoVG/nvgTextAlign vg text-align)
              (NanoVG/nvgTextBounds vg (float 0) (float 0) ^CharSequence text text-bounds))

            (let [xmin (.get text-bounds 0)
                  ymin (.get text-bounds 1)
                  xmax (.get text-bounds 2)
                  ymax (.get text-bounds 3)
                  w (+ (abs xmin) (abs xmax))
                  h (+ (abs ymin) (abs ymax))]
              [w h])))

(defn create-text-node [styles text]
  (let [node (create-node styles)
        [w h] (measure-text styles text)
        set-width (styles-mapping :width)
        set-height (styles-mapping :height)]
    (set-width node w)
    (set-height node h)
    node))

(defn insert-children [ynode children]
  (doseq [idx (range (count children))]
    (Yoga/YGNodeInsertChild ynode (:ynode (nth children idx)) idx))
  (run! layout children))

(defn get-layout* [^long node parent-layout]
  (let [layout (.layout (YGNode/create node))
        x (.positions layout Yoga/YGEdgeLeft)
        y (.positions layout Yoga/YGEdgeTop)
        w (.dimensions layout Yoga/YGDimensionWidth)
        h (.dimensions layout Yoga/YGDimensionHeight)

        [plx ply] parent-layout
        x (+ x plx)
        y (+ y ply)]
    [x y w h]))

(defn store-layout* [ynode parent-layout children]
  (let [layout (get-layout* ynode @parent-layout)]
    (run! #(reset! (:parent-layout %) layout) children)
    (run! store-layout children)))

;; Primitive elements
(defn draw-rounded-rect [vg x y w h border-radius]
  (cond
    (number? border-radius)
    (NanoVG/nvgRoundedRect vg x y w h border-radius)

    (= 4 (count border-radius))
    (let [[tl tr br bl] border-radius]
      (NanoVG/nvgRoundedRectVarying vg x y w h tl tr br bl))

    (= 2 (count border-radius))
    (let [[rv rh] border-radius]
      (NanoVG/nvgRoundedRectVarying vg x y w h rh rh rv rv))))

;; colors pool
(def colors
  {:background-color (NVGColor/calloc)
   :border-color (NVGColor/calloc)
   :text-color (NVGColor/calloc)})

(defn- rect-path [vg x y w h border-radius]
  (NanoVG/nvgBeginPath vg)
  (if border-radius
    (draw-rounded-rect vg x y w h border-radius)
    (NanoVG/nvgRect vg x y w h))
  (NanoVG/nvgClosePath vg))

(defrecord Rect [vg ynode children parent-layout style clip?
                 on-mouse-down on-mouse-up on-mouse-over]
  IDrawable
  (draw [this]
    (let [[x y w h] (get-layout this)
          {:keys [background-color border-width border-color border-radius]} style]

      (rect-path vg x y w h border-radius)

      (when background-color
        (ui.utils/rgba background-color (:background-color colors))
        (NanoVG/nvgFillColor vg (:background-color colors))
        (NanoVG/nvgFill vg))

      (when (and border-color border-width)
        (let [x (+ x border-width)
              y (+ y border-width)
              w (- w (* 2 border-width))
              h (- h (* 2 border-width))]
          ;; insetting border
          (rect-path vg x y w h border-radius)
          (ui.utils/rgba border-color (:border-color colors))
          (NanoVG/nvgStrokeColor vg (:border-color colors))
          (NanoVG/nvgStrokeWidth vg border-width)
          (NanoVG/nvgStroke vg)))

      (when clip? (NanoVG/nvgScissor vg x y w h))
      (run! draw children)
      (when clip? (NanoVG/nvgResetScissor vg))))
  ILayout
  (layout [this]
    (insert-children ynode children))
  (store-layout [this]
    (store-layout* ynode parent-layout children))
  (get-layout [this]
    (get-layout* ynode @parent-layout)))

(defn rect [props & children]
  (map->Rect
   (merge
    props
    {:vg @ui.ctx/vg
     :ynode (create-node (:style props))
     :children children
     :parent-layout (atom [])})))

(def debug-color (ui.utils/rgba 0 150 0 1 (pool/alloc res/colors)))

(defn debug-rect [vg x y w h]
  (doto ^long vg
    (NanoVG/nvgBeginPath)
    (NanoVG/nvgRect x y w h)
    (NanoVG/nvgClosePath)
    (NanoVG/nvgFillColor debug-color)
    (NanoVG/nvgFill)))

(defrecord Text [vg ynode parent-layout text style debug?]
  IDrawable
  (draw [this]
    (let [{:keys [font-size font-face text-color text-align]} style
          [x y w h] (get-layout this)]
      (when debug? (debug-rect vg x y w h))
      (ui.utils/rgba text-color (:text-color colors))
      (doto ^long vg
        (NanoVG/nvgFontSize font-size)
        (NanoVG/nvgFontFace ^CharSequence font-face)
        (NanoVG/nvgFillColor (:text-color colors))
        (NanoVG/nvgTextAlign text-align)
        (NanoVG/nvgText ^float x ^float y ^CharSequence text))))
  ILayout
  (layout [this])
  (store-layout [this])
  (get-layout [this]
    (get-layout* ynode @parent-layout)))

(defn text [props text]
  (map->Text
   (assoc props
          :vg @ui.ctx/vg
          :ynode (create-text-node (:style props) text)
          :text text
          :parent-layout (atom []))))

(def ^:private image-paint (NVGPaint/create))

(defrecord Image [vg ynode parent-layout img]
  IDrawable
  (draw [this]
    (let [[x y w h] (get-layout this)
          _ (NanoVG/nvgImagePattern ^long vg 0 0 w h 0 (:handle img) 1 image-paint)]
      ;; TODO: "inherit" styles from the Rect class
      (doto ^long vg
        (NanoVG/nvgSave)
        (NanoVG/nvgTranslate x y)
        (NanoVG/nvgBeginPath)
        (NanoVG/nvgRect 0 0 w h)
        (NanoVG/nvgClosePath)
        (NanoVG/nvgFillPaint image-paint)
        (NanoVG/nvgFill)
        (NanoVG/nvgRestore))))
  ILayout
  (layout [this])
  (store-layout [this])
  (get-layout [this]
    (get-layout* ynode @parent-layout)))

(defn image [props]
  (let [vg @ui.ctx/vg
        img (ui.utils/create-image-from-file vg (io/file (:src props)))]
    (map->Image
     (assoc props
            :vg vg
            :ynode (create-node (:style props))
            :parent-layout (atom [])
            :img img))))

(defrecord Root [ynode parent-layout style children]
  IDrawable
  (draw [this]
    (run! draw children))
  ILayout
  (layout [this]
    (let [{:keys [width height]} style]
      (insert-children ynode children)
      ;; compute layout from the root
      (Yoga/YGNodeCalculateLayout ynode width height Yoga/YGFlexDirectionColumn)))
  (store-layout [this]
    (store-layout* ynode parent-layout children))
  (get-layout [this]
    (get-layout* ynode @parent-layout)))

(defn root [props & children]
  (map->Root
   (assoc props
          :ynode (create-node (:style props))
          :parent-layout (atom [0 0 (-> props :style :width) (-> props :style :height)])
          :children children)))
