import org.jetbrains.kotlin.plugin.sandbox.MyComposable

fun incorrect(block: <!AMBIGUOUS_FUNCTION_TYPE_KIND!>@MyComposable suspend () -> Unit<!>) {}
