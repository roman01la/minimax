(ns minimax.ui.primitives
  (:require [minimax.ui.context :as ui.ctx])
  (:import (java.nio FloatBuffer)
           (org.lwjgl.nanovg NanoVG)
           (org.lwjgl.system MemoryUtil)
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

(def styles-mapping
  {:flex-direction #(Yoga/YGNodeStyleSetDirection %1 (get-in style-prop-mapping [:flex-direction %2]))
   :justify-content #(Yoga/YGNodeStyleSetJustifyContent %1 (get-in style-prop-mapping [:justify-content %2]))
   :align-items #(Yoga/YGNodeStyleSetAlignItems %1 (get-in style-prop-mapping [:align-items %2]))
   :align-self #(Yoga/YGNodeStyleSetAlignSelf %1 (get-in style-prop-mapping [:align-self %2]))

   :margin (fn [node [top right bottom left]]
             (Yoga/YGNodeStyleSetMargin node Yoga/YGEdgeTop top)
             (Yoga/YGNodeStyleSetMargin node Yoga/YGEdgeRight right)
             (Yoga/YGNodeStyleSetMargin node Yoga/YGEdgeBottom bottom)
             (Yoga/YGNodeStyleSetMargin node Yoga/YGEdgeLeft left))
   :padding (fn [node [top right bottom left]]
              (Yoga/YGNodeStyleSetPadding node Yoga/YGEdgeTop top)
              (Yoga/YGNodeStyleSetPadding node Yoga/YGEdgeRight right)
              (Yoga/YGNodeStyleSetPadding node Yoga/YGEdgeBottom bottom)
              (Yoga/YGNodeStyleSetPadding node Yoga/YGEdgeLeft left))

   :display #(Yoga/YGNodeStyleSetDisplay %1 (get-in style-prop-mapping [:display %2]))
   :position #(Yoga/YGNodeStyleSetPositionType %1 (get-in style-prop-mapping [:position %2]))
   :flex #(Yoga/YGNodeStyleSetFlex %1 %2)

   :height #(Yoga/YGNodeStyleSetHeight %1 %2)
   :width #(Yoga/YGNodeStyleSetWidth %1 %2)

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

(def ^FloatBuffer text-bounds (MemoryUtil/memAllocFloat 4))
(defn measure-text [text]
  (let [_ (NanoVG/nvgTextBounds ^long (deref ui.ctx/vg) (float 0) (float 0) ^CharSequence text text-bounds)
        xmin (.get text-bounds 0)
        ymin (.get text-bounds 1)
        xmax (.get text-bounds 2)
        ymax (.get text-bounds 3)
        w (+ (abs xmin) (abs xmax))
        h (+ (abs ymin) (abs ymax))]
    [w h]))

(defn create-text-node [styles text]
  (let [node (create-node styles)
        [w h] (measure-text text)
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
(defrecord Rect [vg ynode children parent-layout style
                 on-mouse-down on-mouse-up on-mouse-over]
  IDrawable
  (draw [this]
    (let [[x y w h] (get-layout this)
          {:keys [background-color border-width border-color border-radius]} style]
      (NanoVG/nvgBeginPath vg)
      (if border-radius
        (if (number? border-radius)
          (NanoVG/nvgRoundedRect vg x y w h border-radius)
          (let [[tl tr br bl] border-radius]
            (NanoVG/nvgRoundedRectVarying vg x y w h tl tr br bl)))
        (NanoVG/nvgRect vg x y w h))
      (NanoVG/nvgClosePath vg)

      (when background-color
        (NanoVG/nvgFillColor vg background-color)
        (NanoVG/nvgFill vg))

      (when (and border-color border-width)
        (NanoVG/nvgStrokeColor vg border-color)
        (NanoVG/nvgStrokeWidth vg border-width)
        (NanoVG/nvgStroke vg))

      (run! draw children)))
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

(defrecord Text [vg ynode parent-layout text style]
  IDrawable
  (draw [this]
    (let [{:keys [font-size font-face text-color text-align]} style
          [x y w h] (get-layout this)]
      (doto ^long vg
        (NanoVG/nvgFontSize font-size)
        (NanoVG/nvgFontFace ^CharSequence font-face)
        (NanoVG/nvgFillColor text-color)
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
