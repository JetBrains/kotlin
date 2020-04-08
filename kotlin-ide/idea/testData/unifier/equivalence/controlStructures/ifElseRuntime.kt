fun foo(a: Int, b: Int) {
    if ((a) + b > 0) {
        println(a*b)
    }

    println(a*b)

    if (a + b > (0)) {
        println(a*(b))
    }
    else {
        println(a + b)
    }

    if (a + b > 0) {
        println(a*b)
    }

    <selection>if (a + (b) > 0) {
        println(a*b)
    }
    else {
        println((a + b))
    }</selection>
}