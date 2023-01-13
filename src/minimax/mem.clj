(ns minimax.mem
  (:import (org.lwjgl.system MemoryStack)))

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
