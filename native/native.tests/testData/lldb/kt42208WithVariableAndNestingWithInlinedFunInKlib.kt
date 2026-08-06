// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=full
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: optimizationMode=DEBUG
// ^^^ KT-76763: Information on origin of inlined function is lost, when inlining happens on 1st phase.

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