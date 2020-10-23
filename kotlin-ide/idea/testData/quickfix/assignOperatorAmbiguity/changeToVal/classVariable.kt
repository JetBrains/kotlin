// "Change 'list' to val" "false"
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with 'plusAssign()' call
// ACTION: Replace with ordinary assignment
// ERROR: Assignment operators ambiguity: <br>public operator fun <T> Collection<Int>.plus(element: Int): List<Int> defined in kotlin.collections<br>public inline operator fun <T> MutableCollection<in Int>.plusAssign(element: Int): Unit defined in kotlin.collections
// WITH_RUNTIME

class Test {
    var list = mutableListOf(1)

    fun test() {
        list <caret>+= 2
    }
}