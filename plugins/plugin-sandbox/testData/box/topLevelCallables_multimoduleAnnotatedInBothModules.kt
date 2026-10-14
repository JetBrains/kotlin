// MODULE: lib
// FILE: lib.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("libGenerated.kt")
class LibClass

// MODULE: main(lib)
// FILE: main.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("mainGenerated.kt")
class MainClass

fun box(): String {
    val result1 = dummyLibClass(LibClass())
    if (result1 != "foo.LibClass") return "Error: $result1"

    val result2 = dummyMainClass(MainClass())
    if (result2 != "foo.MainClass") return "Error: $result2"

    return "OK"
}
