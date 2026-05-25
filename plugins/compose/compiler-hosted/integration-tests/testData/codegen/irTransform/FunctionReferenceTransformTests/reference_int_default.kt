import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable (Int) -> Int) {
    Ref(::Fn)
}
