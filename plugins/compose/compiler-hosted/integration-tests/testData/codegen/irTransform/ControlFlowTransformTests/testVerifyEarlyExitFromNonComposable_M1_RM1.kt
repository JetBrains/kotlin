import androidx.compose.runtime.Composable

@Composable
fun Test(condition: Boolean) {
    Text("Some text")
    M1 {
        Identity {
            if (condition) return@M1
        }
    }
    Text("Some more text")
}
