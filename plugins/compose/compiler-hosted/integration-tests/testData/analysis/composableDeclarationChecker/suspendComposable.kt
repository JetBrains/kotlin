// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable suspend fun <!COMPOSABLE_SUSPEND_FUN!>Foo<!>() {}

fun acceptSuspend(fn: suspend () -> Unit) { print(fn) }
fun acceptComposableSuspend(fn: <!AMBIGUOUS_FUNCTION_TYPE_KIND!>@Composable suspend () -> Unit<!>) { print(fn.hashCode()) }

val foo: suspend () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> @Composable {}
val bar: suspend () -> Unit = {}
fun Test() {
    val composableLambda = @Composable {}
    acceptSuspend @Composable <!ARGUMENT_TYPE_MISMATCH!>{}<!>
    acceptComposableSuspend @Composable {}
    acceptComposableSuspend(composableLambda)
    acceptSuspend(<!ARGUMENT_TYPE_MISMATCH!><!COMPOSABLE_SUSPEND_FUN!>@Composable <!ANONYMOUS_SUSPEND_FUNCTION!>suspend<!> fun()<!> { }<!>)
}
