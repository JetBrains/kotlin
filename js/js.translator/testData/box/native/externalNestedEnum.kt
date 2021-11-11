// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// KT-46961
// IGNORE_BACKEND: JS

external object O {
    enum class Foo {
        A, B
    }
}

fun box(): String {
    val a = foo(O.Foo.A)
    val b = foo(O.Foo.B)

    if (a != "!") return "fail1: $a"
    if (b != "@") return "fail2: $b"

    return "OK"
}

fun foo(x: O.Foo) = when (x) {
    O.Foo.A -> "!"
    O.Foo.B -> "@"
    else -> ""
}