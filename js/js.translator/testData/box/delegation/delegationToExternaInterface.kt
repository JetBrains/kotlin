// EXPECTED_REACHABLE_NODES: 1236
// KJS_WITH_FULL_RUNTIME
// KT-40126

// MODULE: lib
// FILE: l.kt
@file:Suppress("EXTERNAL_DELEGATION")

@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
external interface MySymbol {
    companion object : MySymbolConstructor by definedExternally
}
external interface MySymbolConstructor {
    @nativeInvoke
    operator fun invoke(description: String = definedExternally): Any
}

// MODULE: main(lib)
// FILE: f.kt

fun foo(ee: MySymbol?) = "OK"

fun box() = foo(null)
