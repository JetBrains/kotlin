// ISSUE: KT-82065
// KIND: STANDALONE_LLDB
// FREE_COMPILER_ARGS: -Xklib-ir-inliner=full

// FILE: lib.kt
inline fun foo(a: Int = 1) = a + 1
inline fun bar(b: Int, c: Int = 2) = b + c

// FILE: main.kt
fun main() {
    foo()
    foo(5)
    bar(10)
    bar(10, 20)
}
