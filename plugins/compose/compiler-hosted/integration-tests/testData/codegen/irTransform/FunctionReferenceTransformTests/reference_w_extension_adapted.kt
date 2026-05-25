import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable Cls.() -> Unit) {
    Ref(Cls::Fn)
}
