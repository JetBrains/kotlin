import androidx.compose.runtime.*

class Unstable(var x: Int)

@Composable fun Test(param: String, unstable: Unstable) {
    Wrapper {
        println(unstable)
    }
}
