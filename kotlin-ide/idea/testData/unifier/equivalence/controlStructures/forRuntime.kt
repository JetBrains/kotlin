fun foo(a: Int, b: Int) {
    <selection>for (i in a - b..a + b) {
        println(i*i)
    }</selection>

    for (k in a - b..a + b) {
        println(k*k)
    }

    for (i in a..a + b) {
        println(i*i)
    }

    for (i in (a - b..a + b)) {
        println(i*i)
    }
}