@Throws(Throwable::class)
fun foo(x: Int) {}

fun foo(x: Long) {}

// Same, but reversed: the second method has `@Throws`, not the first one.
fun bar(x: Int) {}

@Throws(Throwable::class)
fun bar(x: Long) {}
