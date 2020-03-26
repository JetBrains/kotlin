package inline2

inline fun root(crossinline x: () -> String) = object {
    fun run() = x()
}.run()
