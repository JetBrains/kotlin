fun foo() {
    val a: () -> Unit = <selection>{
        val t = 1
        t
    }</selection>

    {
        val v = 1
        v
    }

    val b = {
        val u = 1
        u
    }
}