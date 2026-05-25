import androidx.compose.runtime.*

@Composable
fun Test() {
    val factory = createFactory {
        10
    }
    factory()
}
