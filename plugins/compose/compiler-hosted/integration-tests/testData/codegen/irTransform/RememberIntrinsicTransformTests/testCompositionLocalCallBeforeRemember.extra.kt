import androidx.compose.runtime.Composable


import androidx.compose.runtime.compositionLocalOf

class Foo
class Bar
val compositionLocalBar = compositionLocalOf<Bar> { Bar() }

fun used(x: Any?) {}
