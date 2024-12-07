// ISSUE: KT-64994

// MODULE: a
// FILE: a.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun RMenuItem(
    onClick: (() -> Unit)? = null,
    trailing: (@MyInlineable () -> Unit)? = null,
) {}

// MODULE: b(a)
// FILE: b.kt
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun bar(onClick: (() -> Unit)? = null) {
    RMenuItem(
        onClick = onClick,
        trailing = onClick?.let {
            { }
        }
    )
}

fun box() = "OK"
