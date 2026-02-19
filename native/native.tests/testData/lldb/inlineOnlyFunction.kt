// KIND: STANDALONE_LLDB


fun foo() {}

fun main(args: Array<String>) {
    0.apply {
        foo()
        this.apply {
            foo()
        }
    }
}
