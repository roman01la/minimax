#!/usr/bin/env nu

def main [...args] {
    let class_name = (
        if ($args | length) == 0 {
            'fg.core'
        } else {
            $args | get 0
        }
    )

    with-env { MINIMAX_DEBUG: 1 } {
        clojure -J-Djava.awt.headless=true -J-Dorg.lwjgl.util.Debug=true -M -m $class_name
    }
}