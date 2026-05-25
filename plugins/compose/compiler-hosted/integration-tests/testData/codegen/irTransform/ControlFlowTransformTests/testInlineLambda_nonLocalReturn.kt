import androidx.compose.runtime.*

@Composable
private fun Test(param: String?) {
    Inline1 {
        Inline2 {
            if (true) return@Inline1
        }
    }
}
