// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: JVM, JS_IR

// MODULE: common
// FILE: common.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("commonGenerated.kt")
class CommonClass

// MODULE: platform()()(common)
// FILE: platform.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("platformGenerated.kt")
class PlatformClass

fun box(): String {
    val result1 = dummyCommonClass(CommonClass())
    if (result1 != "foo.CommonClass") return "Error: $result1"

    val result2 = dummyPlatformClass(PlatformClass())
    if (result2 != "foo.PlatformClass") return "Error: $result2"

    return "OK"
}
