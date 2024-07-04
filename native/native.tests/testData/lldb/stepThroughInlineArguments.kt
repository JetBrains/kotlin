// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// INPUT_DATA_FILE: stepThroughInlineArguments.in
// OUTPUT_DATA_FILE: stepThroughInlineArguments.out
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