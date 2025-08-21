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
