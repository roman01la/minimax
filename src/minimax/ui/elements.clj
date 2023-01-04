(ns minimax.ui.elements
  (:import (org.lwjgl.nanovg NVGColor NanoVG)
           (org.lwjgl.util.yoga YGNode Yoga)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def vg (atom nil))

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
                 :space-between Yoga/YGAlignSpaceBetween}})

(def styles-mapping
  {:flex-direction #(Yoga/YGNodeStyleSetDirection %1 (get-in style-prop-mapping [:flex-direction %2]))
   :justify-content #(Yoga/YGNodeStyleSetJustifyContent %1 (get-in style-prop-mapping [:justify-content %2]))
   :align-items #(Yoga/YGNodeStyleSetAlignItems %1 (get-in style-prop-mapping [:align-items %2]))

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

(defn insert-children [ynode children]
  (doseq [idx (range (count children))]
    (Yoga/YGNodeInsertChild ynode (:ynode (nth children idx)) idx)))

(defn get-layout [^long node parent-layout]
  (let [layout (.layout (YGNode/create node))
        x (.positions layout Yoga/YGEdgeLeft)
        y (.positions layout Yoga/YGEdgeTop)
        py (.padding layout 0)
        px (.padding layout 3)
        w (.dimensions layout Yoga/YGDimensionWidth)
        h (.dimensions layout Yoga/YGDimensionHeight)

        [plx ply _ _ plpx plpy] parent-layout
        x (+ x plx plpx)
        y (+ y ply plpy)]
    [x y w h px py]))

(defn rgba ^NVGColor [r g b a ^NVGColor color]
  (.r color (/ r 255))
  (.g color (/ g 255))
  (.b color (/ b 255))
  (.a color a)
  color)

(defn rgba-tag [[r g b a]]
  `(rgba ~r ~g ~b ~a (NVGColor/create)))

(defprotocol IDrawable
  (draw [this parent-layout]))

(defprotocol ILayout
  (layout [this]))

(defrecord UIRect [vg ynode children background-color]
  IDrawable
  (draw [this parent-layout]
    (let [[x y w h :as layout] (get-layout ynode parent-layout)]
      (doto vg
        (NanoVG/nvgBeginPath)
        (NanoVG/nvgRect x y w h)
        (NanoVG/nvgFillColor background-color)
        (NanoVG/nvgFill)
        (NanoVG/nvgClosePath))
      (run! #(draw % layout) children)))
  ILayout
  (layout [this]
    (insert-children ynode children)))

(defn rect [styles & children]
  (map->UIRect (assoc styles :vg @vg :ynode (create-node styles) :children children)))

(defrecord UIText [vg ynode text font-size font-face text-color text-align]
  IDrawable
  (draw [this parent-layout]
    (let [[x y w h] (get-layout ynode parent-layout)]
      (doto ^long vg
        (NanoVG/nvgFontSize font-size)
        (NanoVG/nvgFontFace ^CharSequence font-face)
        (NanoVG/nvgFillColor text-color)
        (NanoVG/nvgTextAlign text-align)
        (NanoVG/nvgText ^float x ^float y ^CharSequence text)))))

(defn text [styles text]
  (map->UIText (assoc styles :vg @vg :ynode (create-node styles) :text text)))

(defrecord UIRoot [ynode width height flex-direction children]
  IDrawable
  (draw [this parent-layout]
    (let [layout (get-layout ynode parent-layout)]
      (run! #(draw % layout) children)))
  ILayout
  (layout [this]
    (insert-children ynode children)
    (Yoga/YGNodeCalculateLayout ynode width height (get-in style-prop-mapping [:flex-direction flex-direction]))))

(defn root [styles & children]
  (map->UIRoot (assoc styles :ynode (create-node styles) :children children)))
