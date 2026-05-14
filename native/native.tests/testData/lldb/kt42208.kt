// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// INPUT_DATA_FILE: kt42208.in
// OUTPUT_DATA_FILE: kt42208.out
// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
