package foo

import org.jetbrains.kotlin.plugin.sandbox.CallSpecifiedFunction

@CallSpecifiedFunction("foo.functionToCall")
fun test(): Int {
    return 42
}

fun box(): String {
    if (result != "Error") return result
    test()
    return result
}
