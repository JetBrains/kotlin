import androidx.compose.runtime.*

val property: @Composable (Int) -> Int = ::Fn

@Composable
fun Ref(content: @Composable (Int) -> Int) {
    Ref(property)
}
