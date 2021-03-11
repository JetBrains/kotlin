fun foo(f: (Int) -> Unit) {
    <selection>{ n: Int ->
        val a = n + 1
        f(a)
    }</selection>

    val x: (Int) -> Unit = {
        val a = it + 1
        f(a)
    }

    { m: Int ->
        val b = m + 1
        f(b)
    }

    val g: (Int) -> Unit = { a ->
        val m = a + 1
        f(m)
    }
}