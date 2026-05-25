import androidx.compose.runtime.*

@Composable
fun Test() {
    InlineWrapperParam {
        Text("Function $it")
    }
}
