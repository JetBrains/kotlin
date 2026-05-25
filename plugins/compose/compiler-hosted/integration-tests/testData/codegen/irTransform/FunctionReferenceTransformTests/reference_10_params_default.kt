import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable (
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int
) -> Unit) {
    Ref(::Fn)
}
