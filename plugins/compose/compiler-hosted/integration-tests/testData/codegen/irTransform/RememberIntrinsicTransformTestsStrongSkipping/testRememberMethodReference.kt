import androidx.compose.runtime.*

@Composable fun Icon(
    param: Int
) {
    val remembered = remember(param::toString) { TODO() }
}
