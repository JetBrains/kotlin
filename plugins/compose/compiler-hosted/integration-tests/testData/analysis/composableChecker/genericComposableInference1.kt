// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun <T> identity(value: T): T = value

// We should infer `ComposableFunction0<Unit>` for `T`
val cl = identity(@Composable {})
val l: () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> cl
