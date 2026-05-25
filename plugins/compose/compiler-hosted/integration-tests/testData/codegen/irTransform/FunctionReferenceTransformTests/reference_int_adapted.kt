import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable () -> Int) {
    Ref(::Fn)
}
