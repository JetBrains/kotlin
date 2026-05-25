// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface A { @Composable fun f() }
typealias B = A

@Composable fun C() { A { C() } }
