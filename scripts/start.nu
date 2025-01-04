#!/usr/bin/env nu

with-env { MINIMAX_DEBUG: 1 } {
    clojure -J-Djava.awt.headless=true -J-Dorg.lwjgl.util.Debug=true -M -m fg.core
}