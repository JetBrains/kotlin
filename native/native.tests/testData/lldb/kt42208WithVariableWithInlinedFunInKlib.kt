// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:+IrIntraModuleInlinerBeforeKlibSerialization -XXLanguage:+IrCrossModuleInlinerBeforeKlibSerialization



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
