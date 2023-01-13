// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -XXLanguage:+UnitConversionsOnArbitraryExpressions
// LLDB_TRACE: kt42208WithPassingLambdaToAnotherFunction.txt
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
fun bar(v:(()->Unit)) {
    v()
}
