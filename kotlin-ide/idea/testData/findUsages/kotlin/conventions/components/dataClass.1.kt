import pack.A

fun test(javaClass: JavaClass) {
    val a = A(1, "2", Any())
    a.n
    a.component1()

    val (x, y, z) = a
    val (x1, y1, z1) = f()
    val (x2, y2, z2) = g()
    val (x3, y3, z3) = h()

    val (x4, y4, z4) = listOfA()[0]

    val (x5, y5, z5) = javaClass.getA()[0]
}

fun f(): A = TODO()
fun g() = A()
fun h() = g()

fun listOfA() = listOf<A>(A(1, "", ""))

fun test2(p1: JavaClass2, p2: JavaClass3) {
    val (x1, y1) = p1[0]
    val (x2, y2) = p2[0]
    val (x3, y3) = JavaClass4.getNested()[0]
}