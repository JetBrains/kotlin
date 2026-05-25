import androidx.compose.runtime.Composable

@Composable
fun Test(a: Boolean, b: Boolean) {
    A()
    M3 {
        A()
        if (a) {
            return
        }
        A()
    }
    M3 {
        A()
        if (b) {
            return
        }
        A()
    }
    A()
}
