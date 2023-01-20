(ns fg.state
  (:import (org.joml Vector3f)))

(set! *warn-on-reflection* true)

(def width 1280)
(def height 800)

(def state
  (atom {;; window
         :width width
         :height height
         :dpr 1
         :minimized? false
         :maximized? false
         ;; resolution
         :vwidth width
         :vheight height
         ;; mouse
         :mx 0
         :my 0
         :mouse-button nil
         :mouse-button-action nil
         ;; scroll
         :sx 0
         :sy 0
         ;; keys
         :key nil
         :key-action nil
         ;; view projection
         :at (Vector3f. 0.0 0.0 0.0)
         :eye (Vector3f. 7.0 6.0 0.0)

         :background-color 0xe9fcff

         :shadow-map-size 1024
         :light-pos (Vector3f. -0.4 -1.0 0.6)}))

(defn reset-state []
  ;; reset mouse button state to make it singular
  (when (or (= 0 (:mouse-button-action @state))
            (= 1 (:mouse-button-action @state)))
    (swap! state assoc
      :mouse-button-action nil
      :mouse-button nil))
  ;; reset mouse scroll
  (swap! state assoc :sx 0 :sy 0))

(defn set-size [dpr]
  (swap! state assoc
         :dpr dpr
         :vwidth (* dpr (:width @state))
         :vheight (* dpr (:height @state))))

(defn add-state-listener [k ks f]
  (add-watch state k (fn [_ _ o n]
                       (let [ov (select-keys o ks)
                             nv (select-keys n ks)]
                         (when (not= ov nv)
                           (f n))))))
