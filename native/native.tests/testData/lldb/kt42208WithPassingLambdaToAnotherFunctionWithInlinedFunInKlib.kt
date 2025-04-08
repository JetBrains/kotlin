// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:+IrInlinerBeforeKlibSerialization
// IGNORE_NATIVE_K2: optimizationMode=DEBUG
// ^^^ KT-76547: WARNING:  Unable to resolve breakpoint to any actual locations.

// FILE: kt42208-1.kt
fun main() {
    val a = foo()
    bar(a)
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
class A
val list = mutableListOf<A>()
inline fun foo() = { ->
    list.add(A())
}
// FILE: kt42208-3.kt
fun bar(v:(()->Boolean)) {
    v()
}
