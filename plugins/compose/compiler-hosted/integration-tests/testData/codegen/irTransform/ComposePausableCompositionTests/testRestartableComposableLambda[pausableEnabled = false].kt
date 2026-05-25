import androidx.compose.runtime.*

@Composable
fun Test(a: Int, b: String, c: Float) {
    Wrap {
        use(a)
        use(b)
        use(c)
    }
}
