(ns minimax.ui.animation)

(defn to-precision [p v]
  (-> (* v p) float Math/round (/ p)))

;; TODO: spring from current position and back
(defn spring [stiffness mass damping from to]
  (let [spring-len (volatile! from)
        spring-rest-len to
        spring-velocity (volatile! 0)
        k (- stiffness)
        d (- damping)]
    (fn [dt]
      (let [spring-force (* k (- @spring-len spring-rest-len))
            spring-damping (* d @spring-velocity)
            spring-acceleration (/ (+ spring-force spring-damping) mass)]
        (vswap! spring-velocity + (* spring-acceleration dt))
        (vswap! spring-len + (* @spring-velocity dt))
        (to-precision 1e2 @spring-len)))))

(defn make-spring [stiffness mass damping]
  (let [s (volatile! nil)
        !from (volatile! nil)]
    (fn [from to dt]
      (when (not= @!from from)
        (vreset! !from from)
        (vreset! s (spring stiffness mass damping from to)))
      (let [f @s]
        (f dt)))))
