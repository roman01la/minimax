(ns minimax.audio.context
  (:require [minimax.audio.source :as audio.source]
            [minimax.audio.utils :as audio.utils])
  (:import (java.util.function IntFunction)
           (org.lwjgl.openal AL AL11 ALC ALC11 ALCapabilities EXTThreadLocalContext)
           (org.lwjgl.system MemoryUtil)))

(defn destroy-audio-context [context device ^ALCapabilities caps use-tlc?]
  (ALC11/alcMakeContextCurrent 0)
  (if use-tlc?
    (AL/setCurrentThread nil)
    (AL/setCurrentProcess nil))
  (MemoryUtil/memFree (.getAddressBuffer caps))
  (ALC11/alcDestroyContext context)
  (ALC11/alcCloseDevice device))

(defprotocol IAudioContext
  (destroy [this])
  (add-source [this name file])
  (remove-source [this name])
  (play-source [this name])
  (pause-source [this name])
  (stop-source [this name])
  (rewind-source [this name])
  (set-gain [this value]))

(deftype AudioContext [context device caps use-tlc? ^:volatile-mutable sources]
  IAudioContext
  (destroy [this]
    (run! audio.source/destroy (vals sources))
    (set! sources {})
    (destroy-audio-context context device caps use-tlc?))
  (add-source [this name file]
    (set! sources (assoc sources name (audio.source/create-source file))))
  (remove-source [this name]
    (audio.source/destroy (sources name))
    (set! sources (dissoc sources name)))
  (play-source [this name]
    (audio.source/play (sources name)))
  (pause-source [this name]
    (audio.source/pause (sources name)))
  (stop-source [this name]
    (audio.source/stop (sources name)))
  (rewind-source [this name]
    (audio.source/rewind (sources name)))
  (set-gain [this value]
    (AL11/alListenerf AL11/AL_GAIN value)))

(defn create-audio-context []
  (let [device (ALC11/alcOpenDevice "")
        _ (assert (some? device) "Failed to open an OpenAL device.")
        caps (ALC/createCapabilities device)
        context (ALC11/alcCreateContext device (MemoryUtil/memCallocInt 1))
        _ (audio.utils/check-alc-error device)
        use-tlc? (and (.-ALC_EXT_thread_local_context caps)
                      (EXTThreadLocalContext/alcSetThreadContext context))
        _ (when-not use-tlc?
            (assert (true? (ALC11/alcMakeContextCurrent context)) "Couldn't init OpenAL context."))
        _ (audio.utils/check-alc-error device)
        caps (AL/createCapabilities caps
               (reify IntFunction
                 (apply [this size]
                   (MemoryUtil/memCallocPointer size))))]
    [context device caps use-tlc?]))

(defn create []
  (let [[context device caps use-tlc?] (create-audio-context)]
    (AudioContext. context device caps use-tlc? {})))
