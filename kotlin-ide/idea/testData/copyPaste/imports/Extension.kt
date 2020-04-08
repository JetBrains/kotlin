package a

fun Int.f() {}

val Int.p: Int
    get() = 2

var Int.g: Int
    get() = 1
    set(i : Int) = Unit

<selection>fun foo() {
    3.f()
    2.p
    3.g = 5
}</selection>