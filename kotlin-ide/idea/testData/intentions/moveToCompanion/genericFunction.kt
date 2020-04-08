// SHOULD_FAIL_WITH: Function <b><code>genericFunT()</code></b> references type parameters of the containing class
class Test6<T>(val t: T) {
    fun <caret>genericFunT(): T = t
}