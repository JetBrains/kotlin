// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable fun A() {}
fun <T> identity(value: T): T = value

// Explicitly instantiate `T` with `ComposableFunction0<Unit>`
val cl = identity<@Composable () -> Unit> { A() }
val l: () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> cl
