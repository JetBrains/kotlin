// DUMP_IR
// ISSUE: KT-77252
// LANGUAGE: +UnnamedLocalVariables

var result = "FAIL: call() must be called"

fun call() {
    result = "OK"
}

val _ = call()

// expected: result: OK