import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun incorrect(block: <!AMBIGUOUS_FUNCTION_TYPE_KIND!>@MyInlineable suspend () -> Unit<!>) {}
