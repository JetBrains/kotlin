// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=disabled
// IGNORE_NATIVE: cacheMode=NO
// IGNORE_NATIVE: cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
// ^^^ See KT-77365 regarding the `mode=` filter.
// INPUT_DATA_FILE: kt42208WithVariableAndNestingWithCache.in
// OUTPUT_DATA_FILE: kt42208WithVariableAndNestingWithCache.out

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
    listAddA()
}
fun listAddA() {
    list.add(A())
}
