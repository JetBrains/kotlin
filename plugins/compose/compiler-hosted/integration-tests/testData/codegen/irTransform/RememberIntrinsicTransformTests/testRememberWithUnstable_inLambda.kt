import androidx.compose.runtime.*

class Unstable(var x: Int)

@Composable fun Test(param: String, unstable: Unstable) {
    Wrapper {
        remember(param, unstable) { param }
    }
}
