import androidx.compose.runtime.*

@Composable fun Icon(
    param: Int,
    defaultParam: Int = LocalColor.current
) {
    val remembered = remember(param, defaultParam) { TODO() }
}
