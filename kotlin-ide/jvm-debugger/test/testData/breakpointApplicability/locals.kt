fun foo1() { /// M
    // We don't support function breakpoints for local functions yet
    fun local() { /// L
        println() /// L
    } /// L
} /// L

fun foo2() { /// M
    val local = fun() { /// L
        println() /// L
    } /// L
} /// L

fun foo3() { /// M
    val local = { /// L
        println() /// L
    } /// L
} /// L

fun foo4() { /// M
    fun local(block: () -> Unit = { println() }) {} /// *, L, λ
} /// L