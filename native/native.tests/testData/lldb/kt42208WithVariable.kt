// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ KT-76547: With pre-serialization inliner, there's an error:
// Failed(reason=Tested process output has not passed validation: The number of responses do not match the number of commands.
// - Commands (8): [b kt42208-2.kt:15, r, bt, c, bt, c, bt, q]
// - Responses (3): [b kt42208-2.kt:15, r, bt] ==> expected: <true> but was: <false>)

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
