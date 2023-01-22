(ns fg.ui
  (:require
   [fg.ui.debug :as ui.debug]
   [minimax.audio.core :as audio]
   [minimax.renderer.ui]
   [minimax.ui.animation :as ui.anim]
   [minimax.ui.components :as mui :refer [defui]]
   [minimax.ui.elements :as ui])
  (:import (org.lwjgl.nanovg NanoVG)))

(defui dialog [props & children]
  (apply ui/view
         (merge
          props
          {:style (merge
                   {:padding [8 8 8 8]
                    :background-color #ui/rgba [230 190 80 1]
                    :border-width 2
                    :border-color #ui/rgba [210 160 35 1]
                    :border-radius 4}
                   (:style props))})
         children))

(defui menu-item [props child-text]
  (ui/view
   {:style {:padding [4 0 4 0]}}
   (ui/text
    {:style {:font-size 14
             :font-face "RobotoSlab-Bold"
             :text-color #ui/rgba [115 90 25 1]
             :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
    child-text)))

;; TODO: stateful UI components
(def button-state (atom nil))
(def button-spring (ui.anim/make-spring 5 1 2))
(def f (volatile! (constantly 1)))
(def click (atom 0))

(defui button [props child-text]
  (let [state (mui/use-state {:hover? false :pressed? false})]
    (mui/view
     {:on-mouse-down (fn []
                       (swap! state assoc :pressed? true)

                       (swap! click inc)
                       (if (odd? @click)
                         (vreset! f #(button-spring 1 2 0.16))
                         (vreset! f #(button-spring 2 1 0.16))))
      :on-mouse-up #(swap! state assoc :pressed? false)
      :on-mouse-enter #(swap! state assoc :hover? true)
      :on-mouse-leave #(swap! state assoc :hover? false)
      :style
      (merge
       {:padding [8 8 8 8]
        :height (* 32 (@f))
        :background-color (cond
                            (:pressed? @state) #ui/rgba [20 150 10 1]
                            (:hover? @state) #ui/rgba [50 230 50 1]
                            :else #ui/rgba [40 210 40 1])
        :border-width 2
        :border-color #ui/rgba [30 70 30 1]
        :border-radius 4
        :justify-content :center
        :align-items :center}
       (:style props)
       {:margin (if (:pressed? @state)
                  [40 0 0 0]
                  (:margin (:style props)))})}
     (ui/text
      {:style {:font-size 16
               :font-face "RobotoSlab-Bold"
               :text-color #ui/rgba [230 250 230 1]
               :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
      child-text))))

(defui game-ui [width height]
  (ui/view
   {:style {:flex 1
            :width width
            :height height
            :flex-direction :column}}
   (ui/view
    {:style {:flex 1
             :justify-content :center}}
    (dialog
     {:style {:width 180
              :margin [0 16 0 0]
              :align-self :flex-end}}
     (menu-item {} "Buildings: 1")
     (menu-item {} "Fruit trees: 2")
     (button {:style {:margin [32 0 0 0]}}
             "Add item")))
   (ui/view
    {:style {:height 64
             :justify-content :center
             :align-items :center}}
    (dialog
     {:style {:padding [8 32 8 32]}}
     (ui/text
      {:style {:font-size 16
               :font-face "RobotoSlab-Bold"
               :text-color #ui/rgba [115 90 25 1]
               :text-align (bit-or NanoVG/NVG_ALIGN_LEFT NanoVG/NVG_ALIGN_TOP)}}
      "Welcome!")))))

(defui sound-button []
  ;; TODO: hookup global state
  (let [gain (mui/use-state 1)
        mute? (zero? @gain)]
    (ui/view
     {:on-mouse-down (fn []
                       (reset! gain (if mute? 1 0))
                       (audio/set-gain @gain))
      :style {:position :absolute
              :right 16
              :bottom 16}}
     (ui/image
      {:src (if mute?
              "resources/images/sound_off.png"
              "resources/images/sound_on.png")
       :style {:width 32
               :height 32}}))))

(defui ui-root [width height scene selected]
  (ui/root
   {:style {:width width
            :height height
            :flex-direction :column}}
   #_(game-ui width height)
   (ui.debug/root width height scene selected)
   (ui/image
    {:src "logo.png"
     :style {:position :absolute
             :left 16
             :bottom 16
             :width (/ 175 3)
             :height (/ 108 3)}})
   (sound-button)))

