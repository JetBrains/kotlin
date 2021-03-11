fun foo(): Unit {
}

fun bar(): Unit {
    return Unit
}

fun baz(): Unit = println("ok")

fun f1(): Int = 1

fun f2(): Unit {
    throw UnsupportedOperationException("")
}

fun f3(): Unit = throw UnsupportedOperationException("")