fun foo(a: Int, b: Int) {
    var t = a
    <selection>do {
        println((t))
        t++
    } while (t != (a + b))</selection>

    var u = a
    do {
        println(t)
        t++
    } while (u != a + b)

    while (t != a + b) {
        println(t)
        t++
    }

    do {
        println(t)
        t++
    } while (t != a + b)
}