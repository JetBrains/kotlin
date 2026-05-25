import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable (String) -> Int) {
    Ref(::Fn)
}
