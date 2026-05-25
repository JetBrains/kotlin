import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable () -> Unit) {
    Ref(::Fn)
}
