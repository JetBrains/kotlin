// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ KT-76547: With pre-serialization inliner, there's an error:
// Failed(reason=Exit code is 1 while 0 was expected.)
// Failed(reason=Tested process output has not passed validation: The number of responses do not match the number of commands.
// - Commands (5): [b kt42208-2.kt:13, r, bt, c, q]
// - Responses (3): [b kt42208-2.kt:13, r, bt] ==> expected: <true> but was: <false>)

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
