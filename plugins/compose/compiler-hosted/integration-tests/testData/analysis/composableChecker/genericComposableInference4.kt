// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun <T> identity(value: T): T = value

// We should infer `T` as `Function0<Unit>` from the context and
// reject the lambda which is explicitly typed as `ComposableFunction...`.
val cl: () -> Unit = identity(@Composable <!ARGUMENT_TYPE_MISMATCH!>{}<!>)
