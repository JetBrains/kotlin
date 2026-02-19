// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=full




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
