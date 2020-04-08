data class <caret>X(val a: Int, val c: Int) {

}

fun test() {
    val (a, c) = X(1, 3)
    val aa = X(1, 3).component1()
    val bb = X(1, 3).component2()
    val cc = X(1, 3).component2()
    for ((a, c) in listOf(X(1, 3))) {}
}