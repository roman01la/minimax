(ns minimax.ui.components
  (:require [minimax.ui.elements :as ui]
            [minimax.glfw :as glfw])
  (:import (org.lwjgl.nanovg NanoVG)))

(def !id (atom 0))
(def !current-element (atom nil))
(def !registry (atom {}))

(defn use-state [v]
  (when-not (contains? @!registry @!current-element)
    (swap! !registry assoc @!current-element (atom v)))
  (get @!registry @!current-element))

(defmacro defui [name args & body]
  `(defn ~name ~args
     (reset! !current-element (str ~(str (ns-name *ns*) "/" name) "::" (swap! !id inc)))
     ~@body))

;; UI components
(defn- scroll-bar [py h height]
  (let [sbw 6
        sbh (- height (- h height))]
    (ui/view
      {:style {:position :absolute
               :top (- py)
               :right 0
               :width sbw
               :height sbh
               :border-radius (/ sbw 2)
               :background-color #ui/rgba [100 100 100 1]}})))

(defui scroll-view [props child]
  (let [state (use-state {:py 0 :h 0})
        {:keys [width height]} (:style props)
        scroll-bar? (> (:h @state) height)]
    (ui/scroll-view
      (merge
        props
        {:on-scroll (fn [sx sy]
                      (when scroll-bar?
                        (let [{:keys [py h]} @state
                              npy (+ py (* sy 10))]
                          (swap! state assoc :py
                            (cond
                              (pos? npy) 0
                              (neg? (+ npy (- h height))) (- height h)
                              :else npy)))))
         :position :relative})
      (ui/view
        {:on-layout (fn [x y w h]
                      (swap! state assoc :h h))
         :style {:top (:py @state)
                 :left 0
                 :position :absolute
                 :width width}}
        child)
      (when scroll-bar?
        (scroll-bar (:py @state) (:h @state) height)))))

(defui view [{:keys [on-mouse-enter on-mouse-leave] :as props} & children]
  (let [mouse-over? (use-state false)
        {:keys [cursor]} (:style props)
        props (assoc props
                :on-mouse-over (fn [hover?]
                                 (when (and (not @mouse-over?) hover?)
                                   (when cursor (glfw/set-cursor cursor))
                                   (when on-mouse-enter (on-mouse-enter)))
                                 (when (and @mouse-over? (not hover?))
                                   (when cursor (glfw/set-cursor :default))
                                   (when on-mouse-leave (on-mouse-leave)))
                                 (reset! mouse-over? hover?)))]
    (apply ui/view props children)))

(defui button [props & children]
  (let [state (use-state {:hover? false :pressed? false})
        {:keys [hover? pressed?]} @state
        style (:style props)
        background-color (or (when hover?
                               (if pressed?
                                 (:active/background-color style)
                                 (:hover/background-color style)))
                             (:background-color style))]
    (apply view
      (merge
        props
        {:on-mouse-down #(swap! state assoc :pressed? true)
         :on-mouse-up #(swap! state assoc :pressed? false)
         :on-mouse-enter #(swap! state assoc :hover? true)
         :on-mouse-leave #(swap! state assoc :hover? false)}
        {:style (assoc (:style props) :background-color background-color)})
      children)))

(defui button-text [props text]
  (let [state (use-state {:hover? false :pressed? false})
        {:keys [hover? pressed?]} @state
        text-style (:text/style props)
        {:keys [on-mouse-down on-mouse-up]} props
        text-color (or (when hover?
                         (if pressed?
                           (:active/text-color text-style)
                           (:hover/text-color text-style)))
                       (:text-color text-style))]
    (view
      (merge
        props
        {:on-mouse-down (fn []
                          (swap! state assoc :pressed? true)
                          (when on-mouse-down (on-mouse-down)))
         :on-mouse-up (fn []
                        (swap! state assoc :pressed? false)
                        (when on-mouse-up (on-mouse-up)))
         :on-mouse-enter #(swap! state assoc :hover? true)
         :on-mouse-leave #(swap! state assoc :hover? false)})
      (ui/text
        {:style (assoc (:text/style props) :text-color text-color)}
        text))))

(defn widget-header [{:keys [on-mouse-down expanded?]} title]
  (ui/view
    {:on-mouse-down on-mouse-down
     :style {:align-items :center
             :flex-direction :row
             :padding [8 0]
             :background-color #ui/rgba [50 50 50 1]
             :border-radius (if expanded? [5 5 0 0] 5)}}
    (ui/text
      {:style {:font-size 12
               :font-face "IBMPlexMono-Regular"
               :text-color #ui/rgba [230 230 230 1]
               :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
      title)))

(defn widget [{:keys [width height on-header-click expanded?]} child]
  (view
    {:style {:width width}}
    (widget-header
      {:on-mouse-down on-header-click
       :expanded? expanded?}
      "Scene Inspector")
    (scroll-view
      {:style {:width width
               :height height
               :background-color #ui/rgba [35 35 35 1]
               :border-radius [0 0 5 5]}}
      child)))
