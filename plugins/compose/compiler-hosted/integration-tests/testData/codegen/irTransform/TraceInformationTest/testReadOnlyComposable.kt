import androidx.compose.runtime.*

@Composable
@ReadOnlyComposable
internal fun someFun(a: Boolean): Boolean {
    if (a) {
        return a
    } else {
        return a
    }
}
