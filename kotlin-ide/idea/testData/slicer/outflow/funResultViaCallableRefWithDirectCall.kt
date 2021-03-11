// FLOW: OUT

fun test() {
    fun bar(n: Int) = <caret>n
    val x = (::bar)(1)
}