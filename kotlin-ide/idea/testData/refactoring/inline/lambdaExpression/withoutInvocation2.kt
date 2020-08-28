// ERROR: Inline Function refactoring cannot be applied to lambda expression without invocation

fun test() {
    val anonFun = <caret>{ x: Int, y: Int -> x + y }
}