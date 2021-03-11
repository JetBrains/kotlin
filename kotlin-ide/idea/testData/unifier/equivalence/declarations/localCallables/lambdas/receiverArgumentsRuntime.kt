fun foo(f: (Int) -> Unit) {
    { m: Int, n: Int, s: String ->
        val a = n + m
        println(s)
        f(a)
    }

    { n: Int, s: String ->
        val a = n + 1
        println(s)
        f(a)
    }

    val q: Int.(Int, String) -> Unit = <selection>{ n: Int, s: String ->
        val a = n + this
        println(s)
        f(a)
    }</selection>

    val g: Int.(Int, String) -> Unit = { a, b ->
        val m = a + this
        println(b)
        f(m)
    }
}