// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58539

// MODULE: common
import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
expect fun ExpectComposable(
    value: String,
    content: @MyComposable (v: String) -> String
): String

fun commonBox(): String {
    return ExpectComposable("O") { it + "K" }
}

// MODULE: platform()()(common)
import org.jetbrains.kotlin.fir.plugin.MyComposable

@MyComposable
actual fun ExpectComposable(
    value: String,
    content: @MyComposable (v: String) -> String
): String {
    return content(value)
}

fun box(): String {
    return commonBox()
}
