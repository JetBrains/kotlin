import androidx.compose.runtime.*

@Composable
private fun KeyContent1(items: List<Int>) {
    items.forEach { item ->
        if (item > -1) {
            key(item) {
                remember {
                    item
                }
            }
        }
    }
}

@Composable
private fun KeyContent2(items: List<Int>) {
    for (item in items) {
        if (item > -1) {
            key(item) {
                remember {
                    item
                }
            }
        }
    }
}
