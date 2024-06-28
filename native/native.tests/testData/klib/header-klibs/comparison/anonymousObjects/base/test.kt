package test

private val x = object { fun f() = 1 }

fun g() =
    x.f() +
        object { fun f() = 0 }.f()
