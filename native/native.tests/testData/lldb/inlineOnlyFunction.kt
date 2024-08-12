// KIND: STANDALONE_LLDB
// FIR_IDENTICAL

fun foo() {}

fun main(args: Array<String>) {
    0.apply {
        foo()
        this.apply {
            foo()
        }
    }
}
