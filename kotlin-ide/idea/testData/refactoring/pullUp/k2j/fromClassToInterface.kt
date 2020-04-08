// WITH_RUNTIME
abstract class <caret>B: I {
    // INFO: {"checked": "true"}
    val x = 1
    // INFO: {"checked": "true"}
    val y: Int get() = 2
    // INFO: {"checked": "true"}
    val z: Int by lazy { 3 }
    // INFO: {"checked": "true"}
    abstract val t: Int
    // INFO: {"checked": "true"}
    fun foo(n: Int): Boolean = n > 0
    // INFO: {"checked": "true"}
    abstract fun bar(s: String)

    // INFO: {"checked": "true"}
    class Y {

    }
}