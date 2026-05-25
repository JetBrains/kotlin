import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable Cls.(Int) -> Unit) {
    Ref(Cls::Fn)
}
