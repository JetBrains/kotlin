// EXPECTED_REACHABLE_NODES: 1309
// KJS_WITH_FULL_RUNTIME

// MODULE: lib
// FILE: lib.kt

interface I
operator fun I.invoke(arg: String): String =
    arg

fun foo(context: I) =
    emptyArray<String>().forEach(context::invoke)

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    foo(object : I {})
    return "OK"
}
