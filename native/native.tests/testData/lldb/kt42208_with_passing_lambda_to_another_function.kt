// TEST_RUNNER: LLDB
// FREE_COMPILER_ARGS: -g -XXLanguage:+UnitConversionsOnArbitraryExpressions
// LLDB_SESSION: kt42208_with_passing_lambda_to_another_function.pat
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
