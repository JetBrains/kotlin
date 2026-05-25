import androidx.compose.runtime.*

@Composable
private fun Test() {
    Bug(listOf(1, 2, 3)) {
        Text(it.toString())
    }
}

@Composable
private inline fun <T> Bug(items: List<T>, content: @Composable (item: T) -> Unit) {
    for (item in items) content(item)
}
