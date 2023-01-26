import org.jetbrains.kotlin.fir.plugin.MyComposable

fun incorrect(block: <!AMBIGUOUS_FUNCTION_TYPE_KIND!>@MyComposable suspend () -> Unit<!>) {}
