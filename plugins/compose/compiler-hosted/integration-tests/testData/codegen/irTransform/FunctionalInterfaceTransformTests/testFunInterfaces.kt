import androidx.compose.runtime.*

fun interface A {
    fun compute(value: Int): Unit
}

@Composable
fun Example(a: A) {
    Example { it -> a.compute(it) }
}
