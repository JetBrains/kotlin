// PROBLEM: none
// ERROR: Assignment operators ambiguity: <br>public operator fun <K, V> Map<out Int, Int>.plus(pair: Pair<Int, Int>): Map<Int, Int> defined in kotlin.collections<br>public inline operator fun <K, V> MutableMap<in Int, in Int>.plusAssign(pair: Pair<Int, Int>): Unit defined in kotlin.collections
// WITH_RUNTIME
fun test() {
    var map = mutableMapOf(1 to 2)
    map <caret>+= 3 to 4
}