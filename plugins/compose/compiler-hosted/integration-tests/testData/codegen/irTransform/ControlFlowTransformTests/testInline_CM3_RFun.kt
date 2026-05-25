import androidx.compose.runtime.Composable

@Composable
fun Test(condition: Boolean) {
    A()
    M3 {
        A()
        if (condition) {
            return
        }
        A()
    }
    A()
}
