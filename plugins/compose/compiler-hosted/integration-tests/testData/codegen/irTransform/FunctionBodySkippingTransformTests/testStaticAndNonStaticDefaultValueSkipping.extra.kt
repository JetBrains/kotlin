import androidx.compose.runtime.Composable


import androidx.compose.runtime.compositionLocalOf

val LocalColor = compositionLocalOf { 123 }
@Composable fun A(a: Int) {}

fun used(x: Any?) {}
