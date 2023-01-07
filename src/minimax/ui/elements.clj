(ns minimax.ui.elements
  (:require [minimax.ui.primitives :as ui.pmt])
  (:import (org.lwjgl.nanovg NVGColor)))

(set! *warn-on-reflection* true)

(defn rgba ^NVGColor [r g b a ^NVGColor color]
  (.r color (/ r 255))
  (.g color (/ g 255))
  (.b color (/ b 255))
  (.a color a)
  color)

(defn rgba-tag [[r g b a]]
  `(rgba ~r ~g ~b ~a (NVGColor/create)))

(defn point-in-rect? [mx my x y w h]
  (and (>= mx x)
       (>= my y)
       (<= mx (+ x w))
       (<= my (+ y h))))

;; UI elements
(defprotocol IEventTarget
  (mouse [this opts]))

(defn get-vnode-children [children]
  (map :vnode children))

(defrecord UIView [vnode props children]
  IEventTarget
  (mouse [this {:keys [mx my mouse-button mouse-button-action] :as opts}]
    (let [[x y w h] (ui.pmt/get-layout vnode)
          mouse-over? (point-in-rect? mx my x y w h)
          {:keys [on-mouse-over on-mouse-up on-mouse-down]} props]
      (when on-mouse-over (on-mouse-over mouse-over?))
      (when mouse-over?
        (case mouse-button-action
          0 (when on-mouse-up (on-mouse-up))
          1 (when on-mouse-down (on-mouse-down))
          nil))
      (run! #(mouse % opts) children))))

(defn view [props & children]
  (map->UIView {:vnode (apply ui.pmt/rect props (get-vnode-children children))
                :props props
                :children children}))



(defrecord UIText [vnode props children]
  IEventTarget
  (mouse [this opts]))

(defn text [props & children]
  (map->UIText {:vnode (apply ui.pmt/text props children)
                :props props
                :children children}))



(defrecord UIRoot [vnode props children]
  IEventTarget
  (mouse [this opts]
    (run! #(mouse % opts) children)))

(defn root [props & children]
  (map->UIRoot {:vnode (apply ui.pmt/root props (get-vnode-children children))
                :props props
                :children children}))
