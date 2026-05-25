// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable fun A() {}
fun <T> identity(value: T): T = value

// We should infer `T` as `ComposableFunction0<Unit>` from the context and then
// infer that the argument to `identity` is a composable lambda.
val cl: @Composable () -> Unit = identity { A() }
