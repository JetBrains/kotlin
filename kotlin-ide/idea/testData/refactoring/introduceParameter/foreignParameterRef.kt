// WITH_RUNTIME
// WITH_DEFAULT_VALUE: false
fun a() {
    1.b(2)
}

fun Int.foo(f: Int.(Int) -> Int) = this

fun Int.b(n: Int) = <selection>n.foo { it + this + n + this@b }</selection> + 1