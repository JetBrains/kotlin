// KIND: STANDALONE_LLDB
// https://youtrack.jetbrains.com/issue/KT-72683
// DISABLE_NATIVE: isAppleTarget=false
// INPUT_DATA_FILE: kt68536.in
// OUTPUT_DATA_FILE: kt68536.out

fun main() {
    val c: I = C()
    val result = c.foo()
    println(result)
}

interface I {
    fun foo(): Any
}

class C : I {
    override fun foo(): Int {
        return 42
    }
}
