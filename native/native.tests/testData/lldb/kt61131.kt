// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: kt61131.in
// OUTPUT_DATA_FILE: kt61131.out
// FIR_IDENTICAL
// FILE: kt61131-1.kt
class FooImpl : Foo {
    override fun bar() = "zzz"
}

fun call_bar(foo: Foo) {
    val s1 = foo.bar()
    val s2 = foo.bar()
    println(s1 + s2)
}

fun main() {
    call_bar(FooImpl())
}

// FILE: kt61131-2.kt
interface Foo {
    fun bar(): String
}