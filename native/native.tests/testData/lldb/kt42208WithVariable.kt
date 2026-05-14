// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=disabled
// FIR_IDENTICAL
// INPUT_DATA_FILE: kt42208WithVariable.in
// OUTPUT_DATA_FILE: kt42208WithVariable.out
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
