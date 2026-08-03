// KIND: STANDALONE_LLDB
// INPUT_DATA_FILE: inlineLambdaRepresentation.in
// OUTPUT_DATA_FILE: inlineLambdaRepresentation.out


inline fun foo(action: () -> Unit) {
    return action()
}

fun bar() = 24

fun main(args: Array<String>) {
    foo {
        bar()
    }
}
