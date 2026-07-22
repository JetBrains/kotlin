
fun foo1() = bar()

fun foo2(body: () -> Unit = ::bar) {
    body()
}

fun foo3(body: () -> Unit = ::bar) = body()

fun bar() {}
