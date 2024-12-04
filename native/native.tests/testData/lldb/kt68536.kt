// KIND: STANDALONE_LLDB
// https://youtrack.jetbrains.com/issue/KT-72683
// DISABLE_NATIVE: isAppleTarget=false
// FIR_IDENTICAL
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
