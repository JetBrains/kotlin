import androidx.compose.runtime.*

@Composable
fun Ref(cls: Cls, content: @Composable () -> Unit) {
    Ref(cls, cls::Fn)
}
