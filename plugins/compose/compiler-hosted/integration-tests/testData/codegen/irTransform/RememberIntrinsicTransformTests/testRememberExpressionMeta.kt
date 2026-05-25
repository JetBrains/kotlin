import androidx.compose.runtime.*

@Composable fun Test(param: String) {
    val a = remember { param }
    Test(a)
}
