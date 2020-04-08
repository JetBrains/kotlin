// PROBLEM: none
// ERROR: Assignment operators ambiguity: <br>public operator fun <T> Set<Int>.plus(element: Int): Set<Int> defined in kotlin.collections<br>public inline operator fun <T> MutableCollection<in Int>.plusAssign(element: Int): Unit defined in kotlin.collections
// WITH_RUNTIME
fun test() {
    var set = mutableSetOf(1)
    set <caret>+= 2
}