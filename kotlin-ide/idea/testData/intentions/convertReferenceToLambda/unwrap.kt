// WITH_RUNTIME

class Wrapper<T>(private val x: T) {
    fun unwrap() = x
}

val unwrapped = listOf(Wrapper(1), Wrapper("B")).map(<caret>Wrapper<out Any>::unwrap)