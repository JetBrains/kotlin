actual interface A<T> {
    actual fun foo(x: T)
    fun foo(x: String)
}

fun main() {
    bar().<!OVERLOAD_RESOLUTION_AMBIGUITY(" public abstract actual fun foo(x: String): Unit defined in A public abstract fun foo(x: String): Unit defined in A")!>foo<!>("")
}
