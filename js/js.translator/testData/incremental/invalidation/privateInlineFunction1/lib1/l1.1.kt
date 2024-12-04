private inline fun privateBar(z: Int, l: (Int) -> Int) = l(z - 5)

private inline fun privateFoo(x: Int, l: (Int) -> Int): Int {
    return privateBar(x * 2, l)
}

fun foo(x: Int, y: Int) = y  - privateFoo(x) { it + it }
