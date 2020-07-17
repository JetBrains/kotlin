// EXPECTED_REACHABLE_NODES: 1462
// KJS_WITH_FULL_RUNTIME

// Reproduction of KT-37563
// Test stack trace capturing in various kinds of constructors

public open class MyExceptionPrimary(message: String) : Exception(message, null) {
    constructor() : this("empty")
}

public open class MyExceptionSecondary : MyExceptionPrimary {
    constructor(message: String) : super(message)
}

@JsName("foo__0")  // Need a stable name to test stack trace text.
fun foo__0() {
    throw Exception("msg", null)
}

@JsName("foo__1")
fun foo__1() {
    throw MyExceptionPrimary("primary")
}

@JsName("foo__2")
fun foo__2(): Throwable {
    throw MyExceptionPrimary()
}

@JsName("foo__3")
fun foo__3(): Throwable {
    throw MyExceptionSecondary("secondaryOnly")
}

fun box(): String {
    val functions = listOf(
        ::foo__0,
        ::foo__1,
        ::foo__2,
        ::foo__3
    )

    var count = 0
    for ((i, f) in functions.withIndex()) {
        try {
            f()
        } catch (e: Throwable) {
            count++
            val stack = e.asDynamic().stack as String

            // Even though stack trace format is not stadard,
            // it should contain names of the functions.
            if (!stack.contains("foo__$i")) return "fail $i"
        }
    }

    if (count != functions.size) return "fail count"

    return "OK"
}