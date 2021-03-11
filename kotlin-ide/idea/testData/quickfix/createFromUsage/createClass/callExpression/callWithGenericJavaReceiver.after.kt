// "Create class 'Foo'" "true"
// ERROR: Unresolved reference: Foo

fun <U> test(u: U) {
    val a = J(u).Foo(u)
}