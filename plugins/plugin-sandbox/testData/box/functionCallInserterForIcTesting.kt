// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
//  ^Reason: KT-82482
// DUMP_IR
// FILE: a.kt
package foo

var result = "Error"

fun functionToCall(): String {
    result = "OK"
    return "OK"
}

// FILE: b.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.CallSpecifiedFunction

@CallSpecifiedFunction("foo.functionToCall")
fun test(): Int {
    val x = 1
    val y = 2
    return x + y
}

fun box(): String {
    if (result != "Error") return result
    val testResult = test()
    if (testResult != 3) return "Error: $testResult"
    return result
}
