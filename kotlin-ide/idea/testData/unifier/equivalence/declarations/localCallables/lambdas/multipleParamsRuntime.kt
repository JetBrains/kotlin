fun foo(f: (Int) -> Unit) {
    <selection>{ n: Int, s: String ->
        val a = n + 1
        println(s)
        f(a)
    }</selection>

    { s: String, n: Int ->
        val a = n + 1
        println(s)
        f(a)
    }

    { m: Int, r: String ->
        val b = m + 1
        println(r)
        f(b)
    }

    val g: (Int, String) -> Unit = { a, b ->
        val m = a + 1
        println(b)
        f(m)
    }
}