(ns minimax.audio.source
  (:require [minimax.audio.utils :as audio.utils]
            [minimax.mem :as mem]
            [minimax.util.fs :as util.fs])
  (:import (java.io File)
           (java.nio IntBuffer ShortBuffer)
           (org.lwjgl.openal AL11)
           (org.lwjgl.stb STBVorbis STBVorbisInfo)
           (org.lwjgl.system MemoryStack)))

(defn read-vorbis ^ShortBuffer [info ^File file]
  (mem/slet [^IntBuffer err [:int 1]]
    (let [buff (util.fs/load-resource file)
          decoder (STBVorbis/stb_vorbis_open_memory buff err nil)
          _ (assert (some? decoder) (str "Failed to open Ogg Vorbis file. Error: " (.get err 0)))
          _ (STBVorbis/stb_vorbis_get_info decoder info)
          channels (.channels info)
          pcm ^ShortBuffer (mem/alloc :short (* channels (STBVorbis/stb_vorbis_stream_length_in_samples decoder)))]
      (STBVorbis/stb_vorbis_get_samples_short_interleaved decoder channels pcm)
      (STBVorbis/stb_vorbis_close decoder)
      pcm)))

;; Source
(defn create-audio-source [file]
  (mem/slet [info STBVorbisInfo]
    (let [buff (AL11/alGenBuffers)
          _ (audio.utils/check-al-error)
          source (AL11/alGenSources)
          _ (audio.utils/check-al-error)
          pcm (read-vorbis info file)
          format (if (= 1 (.channels info)) AL11/AL_FORMAT_MONO16 AL11/AL_FORMAT_STEREO16)]
      (AL11/alBufferData buff format pcm (.sample_rate info))
      (audio.utils/check-al-error)

      (AL11/alSourcei source AL11/AL_BUFFER buff)
      (audio.utils/check-al-error)

      [source buff])))

(defn play-audio-source [source]
  (AL11/alSourcePlay source)
  (audio.utils/check-al-error))

(defn pause-audio-source [source]
  (AL11/alSourcePause source)
  (audio.utils/check-al-error))

(defn stop-audio-source [source]
  (AL11/alSourceStop source)
  (audio.utils/check-al-error))

(defn rewind-audio-source [source]
  (AL11/alSourceRewind source)
  (audio.utils/check-al-error))

(defn destroy-audio-source [source buff]
  (AL11/alSourceStop source)
  (audio.utils/check-al-error)

  (AL11/alDeleteSources ^int source)
  (audio.utils/check-al-error)

  (AL11/alDeleteBuffers ^int buff)
  (audio.utils/check-al-error))

(defprotocol IAudioSource
  (destroy [this])
  (play [this])
  (pause [this])
  (stop [this])
  (rewind [this]))

(deftype AudioSource [source buff file]
  IAudioSource
  (destroy [this]
    (destroy-audio-source source buff))
  (play [this]
    (play-audio-source source))
  (pause [this]
    (pause-audio-source source))
  (stop [this]
    (stop-audio-source source))
  (rewind [this]
    (rewind-audio-source source)))

(defn create-source [file]
  (let [[source buff] (create-audio-source file)]
    (AudioSource. source buff file)))
