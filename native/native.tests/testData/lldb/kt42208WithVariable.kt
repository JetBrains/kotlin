// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:-IrInlinerBeforeKlibSerialization
// ^^^ KT-76547: TODO: Create and fix variant with +IrInlinerBeforeKlibSerialization, where source line 19 has no code anymore:
// WARNING: Unable to resolve breakpoint to any actual locations.

// FILE: kt42208-1.kt
fun main() {
    val a = foo()
    a()
    a()
    a()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
class A
val list = mutableListOf<A>()
inline fun foo() = { ->
    list.add(A())
}
