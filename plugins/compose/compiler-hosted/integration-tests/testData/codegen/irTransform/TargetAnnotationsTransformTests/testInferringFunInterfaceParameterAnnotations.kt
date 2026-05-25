import androidx.compose.runtime.Composable

fun interface CustomComposable {
    @Composable
    fun call()
}

@Composable
fun OpenCustom(content: CustomComposable) {
    content.call()
}

@Composable
fun ClosedCustom(content: CustomComposable) {
    Text("Test")
    content.call()
}

@Composable
fun Test() {
    OpenCustom {
        Text("Test")
    }
    ClosedCustom  {
        Text("Test")
    }
}
