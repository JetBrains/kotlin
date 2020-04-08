fun foo(f: <caret>Int.() -> String) {
    1.f()
    bar(f)
}

fun bar(f: (Int) -> String) {

}

fun lambda(): (Int) -> String = { i -> "$i"}

fun baz(f: (Int) -> String) {
    fun g(i: Int) = ""

    foo(f)

    foo(::g)

    foo(lambda())

    foo { "${this + 1}" }
}