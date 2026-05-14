// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// INPUT_DATA_FILE: kt47198WithBody.in
// OUTPUT_DATA_FILE: kt47198WithBody.out
// FILE: kt47198.kt
fun foo(a:Int){
    print("a: ${'$'}a")
}

fun main() {
    foo(33)
}
