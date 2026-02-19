// KIND: STANDALONE_LLDB

data class Foo(val x: Int)

fun main() {
    val foo = Foo(1)
    val hash = foo.hashCode() // hashCode has no source code, so the debugger shouldn't step into it.
    println(hash)
}
