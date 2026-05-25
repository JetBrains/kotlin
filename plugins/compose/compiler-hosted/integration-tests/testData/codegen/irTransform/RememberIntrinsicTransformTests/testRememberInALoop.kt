import androidx.compose.runtime.*

val count = 0
class SomeUnstableClass(val a: Any = "abc")

val content: @Composable (a: SomeUnstableClass) -> Unit = {
    for (index in 0 until count) {
        val i = remember { index }
    }
    val a = remember { 1 }
}
