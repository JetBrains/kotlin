// KIND: STANDALONE_LLDB
// LLDB_TRACE: stepThroughInlineArguments.txt
// FILE: main.kt
fun bar(x: Int): Int {
    val res = foo(
        x * 2,
        x + 2
    )
    return res
}

fun main() {
    println(bar(42))
}

// FILE: lib.kt
inline fun foo(x: Int, y: Int): Int {
    return x +
            y
}