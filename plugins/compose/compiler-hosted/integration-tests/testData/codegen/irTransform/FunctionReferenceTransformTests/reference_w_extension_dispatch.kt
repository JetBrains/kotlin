import androidx.compose.runtime.*

@Composable
fun Ref(cls: Cls, content: @Composable String.(Int) -> Unit) {
    Ref(cls, cls::Fn)
}
