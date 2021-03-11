interface A {
    val parent: A
}

fun ext(a: A): Int = 1 + ext(a.parent)