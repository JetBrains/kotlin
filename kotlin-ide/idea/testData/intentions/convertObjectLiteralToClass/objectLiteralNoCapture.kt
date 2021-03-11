open class K

fun foo(n: Int) {
    val x = <caret>object : K() {
        fun bar() = 1
    }
}