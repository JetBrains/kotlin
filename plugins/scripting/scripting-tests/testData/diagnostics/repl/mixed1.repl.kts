
// SNIPPET

val foo = "foo"
val bar = 42
fun baz(i: Int) = i + 1

// SNIPPET

class C(val s: String) {
    fun f(): String = s
    fun g(n: Int) = s.length + n
}

// SNIPPET

fun C.sfoo() = s + foo
fun C.sbar() = g(bar)

// SNIPPET

val res1 = C(foo).sfoo()
val res2 = C(foo).sbar()
val res3 = baz(bar)

