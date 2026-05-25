import androidx.compose.runtime.Composable


fun interface A {
    @Composable fun compute(value: Int): Unit
}

fun used(x: Any?) {}
