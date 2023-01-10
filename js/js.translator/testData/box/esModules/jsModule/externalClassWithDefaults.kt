// EXPECTED_REACHABLE_NODES: 1284
// ES_MODULES
// DONT_TARGET_EXACT_BACKEND: JS

// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP

package foo

@JsModule("./externalClassWithDefaults.mjs")
external open class A(ss: String = definedExternally) {
    val s: String
    fun foo(y: String = definedExternally): String = definedExternally
    fun bar(y: String = definedExternally): String = definedExternally
}

class C: A {
    constructor(ss: String) : super(ss) {}
    constructor() : super() {}

    fun qux(s: String = "O") = s
}


fun box(): String {
    val a = A()
    val c = C()

    val r1 = a.foo("O") + c.foo()
    if (r1 != "OK") return "Fail1: $r1"

    val r2 = a.bar() + c.bar("K")
    if (r2 != "OK") return "Fail2: $r2"

    val r3 = c.qux() + c.qux("K")
    if (r3 != "OK") return "Fail3: $r3"

    if (a.s != "A") return "Fail4: ${a.s}"
    if (c.s != "A") return "Fail5: ${c.s}"

    val a2 = A("A2")
    val c2 = C("C2")

    val r6 = a2.s + c2.s
    if (r6 != "A2C2") return "Fail6: $r6"

    return "OK"

}