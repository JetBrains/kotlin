import androidx.compose.runtime.*

@Composable fun Test(param: String, unknown: List<*>) {
    Wrapper {
        println(param)
    }
}
