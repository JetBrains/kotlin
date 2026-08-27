// IGNORE_BACKEND: JS_IR, NATIVE
// IGNORE_HMPP: JS_IR
// ISSUE: KT-64994

// MODULE: a
// FILE: a.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun RMenuItem(
    onClick: (() -> Unit)? = null,
    trailing: (@MyInlineable () -> Unit)? = null,
) {
    onClick?.invoke()
    trailing?.invoke()
}

// MODULE: b(a)
// FILE: b.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

var log = ""

fun bar(onClick: (() -> Unit)? = null) {
    RMenuItem(
        onClick = onClick,
        trailing = onClick?.let {
            { }
        }
    )
}

fun box(): String {
    bar()
    bar(onClick = { log += "O" })
    return log + "K"
}
