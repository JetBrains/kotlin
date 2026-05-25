import androidx.compose.runtime.Composable


import androidx.compose.runtime.Immutable

@Immutable class Foo
@Composable fun A(x: Int) {}
@Composable fun B(y: Foo) {}

fun used(x: Any?) {}
