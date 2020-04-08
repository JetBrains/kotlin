// SHOULD_FAIL_WITH: Package 'default' already contains function f1(Int)
object Test {
    fun <caret>f1(n: Int) {}
}

fun f1(n: Int) {}
