import androidx.compose.runtime.*

@Composable
fun Test() {
    Bug(listOf(1, 2, 3)) {
        Text(it.toString())
    }
}

@Composable
inline fun <T> Bug(items: List<T>, content: @Composable (item: T) -> Unit) {
    for (item in items) content(item)
}
