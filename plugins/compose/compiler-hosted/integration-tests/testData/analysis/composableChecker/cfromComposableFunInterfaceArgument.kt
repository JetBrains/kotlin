// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface A { @Composable fun f() }

@Composable fun B(a: (A) -> Unit) { a { B(a) } }
