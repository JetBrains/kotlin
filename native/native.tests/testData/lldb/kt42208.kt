// KIND: STANDALONE_LLDB
// IGNORE_NATIVE: cacheMode=INCREMENTAL&&mode=TWO_STAGE_MULTI_MODULE
// ^^^ See KT-77365 regarding the `mode=` filter.
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
