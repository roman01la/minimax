(ns minimax.mem
  (:import (org.lwjgl.system MemoryStack MemoryUtil)))

(defn alloc
  "Allocates a buffer of `type` with `count` items on a heap"
  [type count]
  (case type
    :byte (MemoryUtil/memAlloc count)
    :float (MemoryUtil/memAllocFloat count)
    :int (MemoryUtil/memAllocInt count)
    :double (MemoryUtil/memAllocDouble count)
    :short (MemoryUtil/memAllocShort count)
    :long (MemoryUtil/memAllocLong count)))

(defmacro slet
  "Allocates bindings on stack"
  [bindings & body]
  (let [stack-sym (gensym "stack")]
    `(let [~stack-sym (MemoryStack/stackPush)
           ~@(->> (partition 2 bindings)
                  (mapcat (fn [[sym [type c]]]
                            [sym `(~(case type
                                      :double '.mallocDouble
                                      :float '.mallocFloat
                                      :int '.mallocInt
                                      :short '.mallocShort
                                      :long '.mallocLong)
                                    ~stack-sym
                                    ~c)])))
           ret#  (do ~@body)]
       (MemoryStack/stackPop)
       ret#)))
