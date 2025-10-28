// MODULE: main
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JsExport
interface I1 {
    @JsSymbol("toStringTag")
    fun foo(): String
}

@JsExport
interface I2 {
    @JsSymbol("toPrimitive")
    fun foo(): String

    @JsSymbol("replace")
    fun someDefault() =  "OK"
}

@JsExport
class C : I1, I2 {
    override fun foo() = "OK"
}

fun box(): String {
    val c = C();
    var test = c.foo();

    if (test != "OK") return "Failed 1: $test"

    test = c.asDynamic()[js("Symbol.toStringTag")]()

    if (test != "OK") return "Failed 2: $test"

    test = c.asDynamic()[js("Symbol.toPrimitive")]()

    if (test != "OK") return "Failed 3: $test"

    test = c.someDefault()

    if (test != "OK") return "Failed 4: $test"

    test = c.asDynamic()[js("Symbol.replace")]()

    if (test != "OK") return "Failed 5: $test"

    return "OK"
}