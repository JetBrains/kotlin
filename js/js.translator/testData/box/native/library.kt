// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// DONT_TARGET_EXACT_BACKEND: JS_IR
// DONT_TARGET_EXACT_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1282
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package foo

@library class A() {
    @library fun f() {
    }
    @library fun f(a: Int) {
    }
}

@library fun getResult() = false

fun box(): String {
    val a = A()
    a.f()
    a.f(2)
    return if (getResult()) "OK" else "fail"
}
