import androidx.compose.runtime.Composable

@Composable
fun Something(param: (() -> String)?) {
    Something(
        if (param != null) {
            { param() }
        } else {
            null
        }
    )
}
