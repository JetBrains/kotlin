// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:-IrInlinerBeforeKlibSerialization
// ^^^ KT-76547: TODO: Create and fix the variant with +IrInlinerBeforeKlibSerialization, where exception happens at weird kt42208-1.kt:9:72
// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
