data class A(val n: Int, val s: String, val o: Any)

fun test() {
    val a = A(1, "2", Any())
    a.n
    a.component1()
    val (x, y, z) = a
}