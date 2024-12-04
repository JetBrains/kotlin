fun foo(vararg x: Any?): String { return "OK" }

fun box(): String {
    return foo()
}