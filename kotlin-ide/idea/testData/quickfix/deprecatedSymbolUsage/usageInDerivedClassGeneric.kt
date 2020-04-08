// "Replace with 'newFun(t, k)'" "true"

open class Base<T> {
    @Deprecated("", ReplaceWith("newFun(t, k)"))
    fun <K> oldFun(t: T, k: K) = k

    fun <K> newFun(t: T, k: K) = k
}

class Derived : Base<String>() {
    fun foo() {
        <caret>oldFun("a", 1)
    }
}
