import androidx.compose.runtime.Composable


import androidx.compose.runtime.*

enum class Foo {
    A,
    B,
    C,
}

fun swizzle(a: Int, b: Int) = a + b

@Composable
fun used(a: Any) { }

fun used(x: Any?) {}
