fun foo(a: Int, b: Int) {
    <selection>if ((a) + b > 0) {
        println(a*(b))
    }</selection>

    println(a*b)

    if (a + b > 0) {
        println(a*(b))
    }

    if (a + b > 0) {
        println((a*b))
    }
    else {
        println(a + b)
    }
}