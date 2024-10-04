// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58539

// MODULE: common
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
expect fun ExpectInlineable(
    value: String,
    content: @MyInlineable (v: String) -> String
): String

fun commonBox(): String {
    return ExpectInlineable("O") { it + "K" }
}

// MODULE: platform()()(common)
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

@MyInlineable
actual fun ExpectInlineable(
    value: String,
    content: @MyInlineable (v: String) -> String
): String {
    return content(value)
}

fun box(): String {
    return commonBox()
}
