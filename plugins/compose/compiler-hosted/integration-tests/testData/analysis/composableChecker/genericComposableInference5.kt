// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun <T> identity(value: T): T = value

// We should infer `Function0<Unit>` for `T`
val lambda = identity<() -> Unit>(@Composable <!ARGUMENT_TYPE_MISMATCH!>{}<!>)
