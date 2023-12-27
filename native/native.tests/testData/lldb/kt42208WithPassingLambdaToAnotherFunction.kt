// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -XXLanguage:+UnitConversionsOnArbitraryExpressions
// IGNORE_BACKEND_K2: NATIVE
// FIR_IDENTICAL
// Not really `FIR_IDENTICAL`, because we ignore thois test for FIR, but with this directive presented we don't need to create separate `fir.txt` file for dump.

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
