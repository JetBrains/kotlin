// TEST_RUNNER: LLDB
// LLDB_SESSION: kt47198_with_body.pat
// FILE: kt47198.kt
fun foo(a:Int){
    print("a: ${'$'}a")
}

fun main() {
    foo(33)
}
