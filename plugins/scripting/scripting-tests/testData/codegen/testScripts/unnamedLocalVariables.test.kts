// DUMP_IR
// ISSUE: KT-77252
// LANGUAGE: +UnnamedLocalVariables

var result = "FAIL: call() must be called"

fun call(): Int {
    result = "OK"
    return 0
}

val _ = call()

// expected: result: OK
