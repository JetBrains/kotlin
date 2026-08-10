// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inlineOnlyFunction.in
// OUTPUT_DATA_FILE: inlineOnlyFunction.out


fun foo() {}

fun main(args: Array<String>) {
    0.apply {
        foo()
        this.apply {
            foo()
        }
    }
}
