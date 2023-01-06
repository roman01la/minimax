(ns minimax.ui.elements
  (:import (java.nio FloatBuffer)
           (org.lwjgl.nanovg NVGColor NanoVG)
           (org.lwjgl.system MemoryUtil)
           (org.lwjgl.util.yoga YGNode Yoga)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; NanoVG context
(def vg (atom nil))

(defprotocol IDrawable
  (draw [this]))

(defprotocol ILayout
  (layout [this])
  (store-layout [this]))

(defprotocol IEventTarget
  (mouse [this opts]))

(def color
  (NVGColor/create))

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
  (let [_ (NanoVG/nvgTextBounds ^long (deref vg) (float 0) (float 0) ^CharSequence text text-bounds)
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

(defn get-layout [^long node parent-layout]
  (let [layout (.layout (YGNode/create node))
        x (.positions layout Yoga/YGEdgeLeft)
        y (.positions layout Yoga/YGEdgeTop)
        w (.dimensions layout Yoga/YGDimensionWidth)
        h (.dimensions layout Yoga/YGDimensionHeight)

        [plx ply] parent-layout
        x (+ x plx)
        y (+ y ply)]
    [x y w h]))

(defn rgba ^NVGColor [r g b a ^NVGColor color]
  (.r color (/ r 255))
  (.g color (/ g 255))
  (.b color (/ b 255))
  (.a color a)
  color)

(defn rgba-tag [[r g b a]]
  `(rgba ~r ~g ~b ~a (NVGColor/create)))

;; Vector Elements

(defn store-layout* [ynode parent-layout children]
  (let [layout (get-layout ynode @parent-layout)]
    (run! #(reset! (:parent-layout %) layout) children)
    (run! store-layout children)))

(defn point-in-rect? [mx my x y w h]
  (and (>= mx x)
       (>= my y)
       (<= mx (+ x w))
       (<= my (+ y h))))

(defrecord UIRect [vg ynode children state parent-layout style on-mouse-down on-mouse-up]
  IDrawable
  (draw [this]
    (let [[x y w h] (get-layout ynode @parent-layout)
          {:keys [background-color border-width border-color border-radius]} style
          background-color (or
                             (when (:hover? @state)
                               (if (:active? @state)
                                 (:active/background-color style)
                                 (:hover/background-color style)))
                             background-color)]
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
  IEventTarget
  (mouse [this {:keys [mx my mouse-button mouse-button-action] :as opts}]
    (let [[x y w h] (get-layout ynode @parent-layout)
          active? (and (= 0 mouse-button) (= 1 mouse-button-action))
          hover? (point-in-rect? mx my x y w h)]
      ;; TODO: click event (stateful)
      ;; TODO: continuous pressed state
      (when hover?
        (case mouse-button-action
          0 (when on-mouse-up (on-mouse-up))
          1 (when on-mouse-down (on-mouse-down))
          nil))
      (swap! state assoc :active? active? :hover? hover?)
      (run! #(mouse % opts) children))))

(defn rect [props & children]
  (map->UIRect
    (merge
      props
      {:vg @vg
       :ynode (create-node (:style props))
       :children children
       :state (atom {:hover? false :active? false})
       :parent-layout (atom [])})))

(defrecord UIText [vg ynode parent-layout text font-size font-face text-color text-align]
  IDrawable
  (draw [this]
    (let [[x y w h] (get-layout ynode @parent-layout)]
      (doto ^long vg
        (NanoVG/nvgFontSize font-size)
        (NanoVG/nvgFontFace ^CharSequence font-face)
        (NanoVG/nvgFillColor text-color)
        (NanoVG/nvgTextAlign text-align)
        (NanoVG/nvgText ^float x ^float y ^CharSequence text))))
  ILayout
  (layout [this])
  (store-layout [this])
  IEventTarget
  (mouse [this opts]))

(defn text [styles text]
  (map->UIText
    (assoc styles
      :vg @vg
      :ynode (create-text-node styles text)
      :text text
      :parent-layout (atom []))))

(defrecord UIRoot [ynode parent-layout width height flex-direction children]
  IDrawable
  (draw [this]
    (run! draw children))
  ILayout
  (layout [this]
    (insert-children ynode children)
    (Yoga/YGNodeCalculateLayout ynode width height (get-in style-prop-mapping [:flex-direction flex-direction])))
  (store-layout [this]
    (store-layout* ynode parent-layout children))
  IEventTarget
  (mouse [this opts]
    (run! #(mouse % opts) children)))

(defn root [styles & children]
  (map->UIRoot
    (assoc styles
      :ynode (create-node styles)
      :parent-layout (atom [0 0 (:width styles) (:height styles)])
      :children children)))
