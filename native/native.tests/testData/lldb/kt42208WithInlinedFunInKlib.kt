// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:+IrInlinerBeforeKlibSerialization
// IGNORE_NATIVE_K2: optimizationMode=DEBUG
// ^^^ KT-76763: lost attributeOwnerId leads to misdetecting the origin of inlined function, thus corruption of fileEntry in debug info for `invoke` function.
// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
