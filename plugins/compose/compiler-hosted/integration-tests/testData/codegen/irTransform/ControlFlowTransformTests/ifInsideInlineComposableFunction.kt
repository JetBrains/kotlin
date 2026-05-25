import androidx.compose.runtime.*

@Composable
fun Label(test: Boolean) {
    Layout(
        content = {
            Box()
            if (test) {
                Box()
            }
        }
    )
}
