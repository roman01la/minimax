(ns minimax.audio.utils
  (:import (org.lwjgl.openal AL11 ALC11)))

(defn check-al-error []
  (let [err (AL11/alGetError)]
    (assert (= AL11/AL_NO_ERROR err) (AL11/alGetString err))))

(defn check-alc-error [device]
  (let [err (ALC11/alcGetError device)]
    (assert (= ALC11/ALC_NO_ERROR err) (ALC11/alcGetString device err))))
