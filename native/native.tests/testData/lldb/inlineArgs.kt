// KIND: STANDALONE_LLDB


inline fun foo(x: Int, y: Int, z: Int = 5): Int {
    return x + y
}

fun bar() = 24

fun main(args: Array<String>) {
    val x = 42
    foo(1, bar())
}
