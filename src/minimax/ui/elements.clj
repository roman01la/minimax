(ns minimax.ui.elements
  (:require [minimax.ui.primitives :as ui.pmt]
            [minimax.ui.utils :as ui.utils]
            [minimax.pool.core :as pool]
            [minimax.resources :as res]))

(set! *warn-on-reflection* true)

(defn rgba-tag [v]
  #_`(ui.utils/rgba ~r ~g ~b ~a (pool/alloc res/colors))
  v)

;; UI elements
(defprotocol IEventTarget
  (ui-event [this opts]))

(defn get-vnode-children [children]
  (map :vnode children))

(defn flatten-children [children]
  (->> children
       (mapcat #(if (seq? %) % [%]))
       (filter some?)))

(defn parent-child-intersect? [layout child]
  (ui.utils/rects-intersect? (ui.pmt/get-layout (:vnode child)) layout))

(defn propagate-events [opts layout children]
  (doseq [child children]
    (if-not (:clip? opts)
      (ui-event child opts)
      (when (parent-child-intersect? layout child)
        (ui-event child opts)))))

(defrecord UIView [vnode props children]
  IEventTarget
  (ui-event [this {:keys [mx my mouse-button mouse-button-action] :as opts}]
    (let [[x y w h :as layout] (ui.pmt/get-layout vnode)
          mouse-over? (ui.utils/point-in-rect? mx my x y w h)
          {:keys [on-mouse-over on-mouse-up on-mouse-down on-layout]} props]
      (when on-layout (on-layout x y w h))
      (when on-mouse-over (on-mouse-over mouse-over?))
      (when mouse-over?
        (case mouse-button-action
          0 (when on-mouse-up (on-mouse-up))
          1 (when on-mouse-down (on-mouse-down))
          nil))
      (propagate-events opts layout children))))

(defn view [props & children]
  (let [children (flatten-children children)]
    (map->UIView {:vnode (apply ui.pmt/rect props (get-vnode-children children))
                  :props props
                  :children children})))

(defrecord UIScrollView [vnode props children]
  IEventTarget
  (ui-event [this {:keys [sx sy mx my] :as opts}]
    (let [[x y w h :as layout] (ui.pmt/get-layout vnode)
          mouse-over? (ui.utils/point-in-rect? mx my x y w h)
          {:keys [on-scroll]} props
          opts (assoc opts :clip? true)]
      (when (and mouse-over? on-scroll
                 (not (zero? (+ sx sy))))
        (on-scroll sx sy))
      (propagate-events opts layout children))))

(defn scroll-view [props & children]
  (let [children (flatten-children children)]
    (map->UIScrollView {:vnode (apply ui.pmt/rect (assoc props :clip? true) (get-vnode-children children))
                        :props props
                        :children children})))


(defrecord UIText [vnode props children]
  IEventTarget
  (ui-event [this opts]))

(defn text [props & children]
  (let [children (flatten-children children)]
    (map->UIText {:vnode (apply ui.pmt/text props children)
                  :props props
                  :children children})))


(defrecord UIImage [vnode props]
  IEventTarget
  (ui-event [this opts]))

(defn image [props]
  (map->UIText {:vnode (ui.pmt/image props)
                :props props}))


(defrecord UIRoot [vnode props children]
  IEventTarget
  (ui-event [this opts]
    (run! #(ui-event % opts) children)))

(defn root [props & children]
  (let [children (flatten-children children)]
    (map->UIRoot {:vnode (apply ui.pmt/root props (get-vnode-children children))
                  :props props
                  :children children})))
