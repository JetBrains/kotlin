// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: JVM, JS_IR

// MODULE: common
// FILE: common.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("commonGenerated.kt")
interface CommonInterface

@DummyFunction("commonGenerated.kt")
class CommonClass : CommonInterface

@DummyFunction("commonGenerated.kt")
object CommonObject

// MODULE: platform()()(common)
// FILE: platform.kt
package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction("platformGenerated.kt")
class PlatformClass

fun box(): String {
    val result1 = dummyCommonInterface(CommonClass())
    if (result1 != "foo.CommonInterface") return "Error: $result1"

    val result2 = dummyCommonClass(CommonClass())
    if (result2 != "foo.CommonClass") return "Error: $result2"

    val result3 = dummyPlatformClass(PlatformClass())
    if (result3 != "foo.PlatformClass") return "Error: $result3"

    return "OK"
}
