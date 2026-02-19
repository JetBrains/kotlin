// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=full



// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
