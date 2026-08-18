// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: stepIntoSyntheticFunction.in
// OUTPUT_DATA_FILE: stepIntoSyntheticFunction.out

data class Foo(val x: Int)

fun main() {
    val foo = Foo(1)
    val hash = foo.hashCode() // hashCode has no source code, so the debugger shouldn't step into it.
    println(hash)
}
