import androidx.compose.runtime.*

fun interface A {
    @Composable fun compute(value: Int): Unit
}
fun Example(a: A) {
    Example { it -> a.compute(it) }
}
