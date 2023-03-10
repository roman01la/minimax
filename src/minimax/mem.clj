(ns minimax.mem
  (:import (org.lwjgl.system MemoryStack MemoryUtil)))

;; TODO: free allocated memory
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
  "Allocates bindings on stack.
  Values in bindings can be:
  - A tuple of keyword of a primitive type #{:double :float :int :short :long} and buffer size
  - Name of a class that supports stack allocation via a static method `malloc(MemoryStack stack)`"
  [bindings & body]
  (let [stack-sym (gensym "stack")]
    `(let [~stack-sym (MemoryStack/stackPush)
           ~@(->> (partition 2 bindings)
                  (mapcat (fn [[sym v]]
                            (if (symbol? v)
                              [sym `(~(symbol (str v) "/malloc") ~stack-sym)]
                              (let [[type c] v]
                                [sym `(~(case type
                                          :double '.mallocDouble
                                          :float '.mallocFloat
                                          :int '.mallocInt
                                          :short '.mallocShort
                                          :long '.mallocLong)
                                       ~stack-sym
                                       ~c)])))))
           ret#  (do ~@body)]
       (MemoryStack/stackPop)
       ret#)))
