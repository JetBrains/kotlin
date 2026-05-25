import androidx.compose.runtime.Composable
@Composable fun Example(x: Int) {
    fun foo() { use(x) }
    val shouldMemoize: ()->(()->Unit) = { ::foo }
}
