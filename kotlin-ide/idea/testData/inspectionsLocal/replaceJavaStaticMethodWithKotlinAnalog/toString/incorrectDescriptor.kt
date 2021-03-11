// RUNTIME_WITH_FULL_JDK
// PROBLEM: none
// ERROR: None of the following functions can be called with the arguments supplied: <br>public open fun toString(i: Int): String! defined in java.lang.Integer<br>public open fun toString(i: Int, radix: Int): String! defined in java.lang.Integer

fun main() {
    println(Integer.toString(<caret>))
}
