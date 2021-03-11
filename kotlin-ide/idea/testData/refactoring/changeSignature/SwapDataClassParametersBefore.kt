data class <caret>X(val a: Int, val b: Int, val c: Int) {

}

fun test() {
    val (a, b, c) = X(1, 2, 3)
    val aa = X(1, 2, 3).component1()
    val bb = X(1, 2, 3).component2()
    val cc = X(1, 2, 3).component3()
    for ((a, b, c) in listOf(X(1, 2, 3))) {}
}