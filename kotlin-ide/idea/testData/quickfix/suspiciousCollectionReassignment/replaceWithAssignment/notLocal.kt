// "Replace with assignment (original is empty)" "false"
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_RUNTIME
class Test {
    var list = emptyList<Int>()
    fun test(otherList: List<Int>) {
        list +=<caret> otherList
    }
}
