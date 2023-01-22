(ns minimax.passes
  (:require [minimax.renderer.view :as view]))

(def shadow (view/create 0))
(def geometry (view/create 1))
(def combine (view/create 2))
(def blit (view/create 3))
(def ui (view/create 4))
