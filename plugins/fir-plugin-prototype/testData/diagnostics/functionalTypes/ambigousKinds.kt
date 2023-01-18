import org.jetbrains.kotlin.fir.plugin.MyComposable

fun incorrect(block: <!AMBIGUOUS_FUNCTIONAL_TYPE_KIND!>@MyComposable suspend () -> Unit<!>) {}
