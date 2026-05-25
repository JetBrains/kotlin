import androidx.compose.runtime.Composable

@Composable
fun Test(condition: Boolean) {
    A()
    M3 {
        A()
        if (condition) {
            return@M3
        }
        A()
    }
    A()
}
