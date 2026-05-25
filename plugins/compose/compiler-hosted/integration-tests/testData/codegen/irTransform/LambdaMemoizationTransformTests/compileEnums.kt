import androidx.compose.runtime.*

enum class Test {
    A, B, C
}

@Composable
fun Test(parameter: Test) {
    val lambda = { println(parameter) }
}
