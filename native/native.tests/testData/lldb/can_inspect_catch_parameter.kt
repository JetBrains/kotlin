// TEST_RUNNER: LLDB
// LLDB_SESSION: can_inspect_catch_parameter.pat
fun main() {
    try {
        throw Exception("message 1")
    } catch (e1: Throwable) {
        println(e1.message)
    }

    try {
        throwError()
    } catch (e2: Throwable) {
        println(e2.message)
    }
}

fun throwError() {
    throw Error("message 2")
}
