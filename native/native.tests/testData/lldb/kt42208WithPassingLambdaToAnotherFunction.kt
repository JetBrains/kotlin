// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=disabled
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE&&mode=TWO_STAGE_MULTI_MODULE
// ^^^ See KT-77365 regarding the `mode=` filter.
// INPUT_DATA_FILE: kt42208WithPassingLambdaToAnotherFunction.in
// OUTPUT_DATA_FILE: kt42208WithPassingLambdaToAnotherFunction.out
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
