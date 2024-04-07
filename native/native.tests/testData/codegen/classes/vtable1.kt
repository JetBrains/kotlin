abstract class Super {
    abstract fun bar(): String
}

class Foo : Super() {
    final override fun bar(): String { return "OK" }
}

private fun create(): Super {
    return Foo()
}

fun box(): String {
    return create().bar()
}