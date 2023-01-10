// EXPECTED_REACHABLE_NODES: 1334
// ES_MODULES
// DONT_TARGET_EXACT_BACKEND: JS

// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

package foo

@JsModule("./externalConstructor.mjs")
open external class A(data: String) {
    constructor(data: Int)
    constructor(data: Boolean)

    val data: Any
}

class B(data: String) : A(data)

class C(data: Int) : A(data)

class D(data: Boolean) : A(data)

fun box(): String {
    assertEquals("13", B("13").data)
    assertEquals(42, C(42).data)
    assertEquals(true, D(true).data)
    return "OK"
}
