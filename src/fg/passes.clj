(ns fg.passes
  (:require [minimax.view :as view]))

(def shadow (view/create 0))
(def geometry (view/create 1))
(def picking (view/create 2))
(def combine (view/create 3))
(def blit (view/create 4))
