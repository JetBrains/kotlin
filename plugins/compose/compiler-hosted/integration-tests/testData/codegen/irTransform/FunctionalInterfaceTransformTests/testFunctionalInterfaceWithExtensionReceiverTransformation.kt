import androidx.compose.runtime.*
fun interface TestContent {
    @Composable
    fun String.Content()
}
@Composable
fun Test(content: TestContent) {
    with(content) {
        "".Content()
    }
}

@Composable
fun CallTest() {
    Test { this.length }
}
