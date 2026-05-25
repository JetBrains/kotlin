import androidx.compose.runtime.Composable

@Composable
fun Test(condition: Boolean) {
    Text("Some text")
    Identity {
        if (condition) return@Test
    }
    Text("Some more text")
}
