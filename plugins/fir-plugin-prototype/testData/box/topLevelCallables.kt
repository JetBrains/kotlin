// FIR_DISABLE_LAZY_RESOLVE_CHECKS
package foo

import org.jetbrains.kotlin.fir.plugin.DummyFunction

@DummyFunction
class First

@DummyFunction
class Second

fun box(): String {
    val result1 = dummyFirst(First())
    if (result1 != "foo.First") return "Error: $result1"

    val result2 = dummySecond(Second())
    if (result2 != "foo.Second") return "Error: $result2"

    return "OK"
}
