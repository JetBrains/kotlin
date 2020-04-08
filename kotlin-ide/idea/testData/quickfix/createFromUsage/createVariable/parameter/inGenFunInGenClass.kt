// "Create parameter 'foo'" "true"

class A<T> {
    fun <T> test(n: Int) {
        val t: T = <caret>foo
    }
}
