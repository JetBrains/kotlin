data class <caret>X(val a: Int, val b: Int) {

}

fun test() {
    val (a, b) = X(1, 2)
    val aa = X(1, 2).component1()
    val bb = X(1, 2).component2()
    for ((a, b) in listOf(X(1, 2))) {}
}