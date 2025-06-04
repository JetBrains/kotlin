// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// FREE_COMPILER_ARGS: -XXLanguage:-IrInlinerBeforeKlibSerialization
// ^^^ KT-76763: When fixed, please remove FREE_COMPILER_ARGS directive, and test `kt42208WithInlinedFunInKlib.kt`
// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
