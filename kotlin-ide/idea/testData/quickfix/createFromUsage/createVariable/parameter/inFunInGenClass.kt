// "Create parameter 'foo'" "true"

class A<T> {
    fun test(n: Int) {
        val t: T = <caret>foo
    }
}
