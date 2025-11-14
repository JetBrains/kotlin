// IGNORE_BACKEND: JS_IR, NATIVE
// IGNORE_HMPP: JS_IR
// IGNORE_NATIVE: mode=ONE_STAGE_MULTI_MODULE
//  ^Reason: KT-82482
// FILE: foo/some.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("first.kt")
class First

@DummyFunction("first")
class OtherFirst

@DummyFunction("second")
class Second

@DummyFunction
class Third

fun box(): String {
    val result1 = dummyFirst(First())
    if (result1 != "foo.First") return "Error: $result1"

    val result2 = dummySecond(Second())
    if (result2 != "foo.Second") return "Error: $result2"

    return "OK"
}

// FILE: bar/other.kt
package bar

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("first.kt")
class First
