import androidx.compose.runtime.*

@Composable
fun Ref(cls: Cls, content: @Composable (Int) -> Unit) {
    Ref(cls, cls::Fn)
}
