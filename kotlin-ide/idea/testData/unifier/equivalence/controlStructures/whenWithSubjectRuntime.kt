fun foo(a: Int, b: Int) {
    <selection>when((a)) {
        1 -> println(b)
        (2) -> println((a - b))
        else -> println(a + b)
    }</selection>

    when(a) {
        1 -> println(b)
        2 -> println(a - b)
    }

    when {
        a == 1 -> println(b)
        a == 2 -> println(a - b)
        else -> println(a + b)
    }

    when(a) {
        1 -> println(b)
        2 -> println(a - b)
        else -> println(a + b)
    }

    when(a) {
        2 -> println(a - b)
        1 -> println(b)
    }
}