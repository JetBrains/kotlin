package org.jetbrains.uast.kotlin

fun foo() {

    val lam1 = { a: Int ->
        val b = 1
        a + b
    }

    val lam2 = { a: Int ->
        val c = 1
        if (a > 0)
            a + c
        else
            a - c
    }

    val lam3 = lbd@{ a: Int ->
        val d = 1
        return@lbd a + d
    }

    val lam4 = fun(a: Int): String {
        if (a < 5) return "5"

        if (a > 0)
            return "1"
        else
            return "2"
    }

    val lam5 = fun(a: Int) = "a" + a

    bar {
        if (it > 5) return
        val b = 1
        it + b
    }

    val x: () -> Unit = {
        val (a, b) = listOf(1, 2)
    }

    val y: () -> Unit = {
        listOf(1)
    }

}

private inline fun bar(lmbd: (Int) -> Int) {
    lmbd(1)
}
