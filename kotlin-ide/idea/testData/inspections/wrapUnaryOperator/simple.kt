object Constants {
    val ONE = 1
}

fun foo() {
    println(-1.inc())
    val i = 100
    println(-i.inc())
    println(-1.1.dec())
    println(-1.1f.dec())
    println(1 - -1.dec().dec())
    println(1 - -1.javaClass)
    println(1 - - 1.dec().dec())
    println(1 - - 1.javaClass)
    println(1-1.inc()) // NG
    println(1 - 1.dec().dec()) // NG
    println(1 - 1.javaClass) // NG
    println(-1) //NG
    println(!true) // NG
    println(!true.not()) // NG
    println(+1) // NG
    println(+1.inc())
    println(+3.14.inc())

    println(-Constants.ONE) // NG
    println(-"2".toInt()) // NG
}