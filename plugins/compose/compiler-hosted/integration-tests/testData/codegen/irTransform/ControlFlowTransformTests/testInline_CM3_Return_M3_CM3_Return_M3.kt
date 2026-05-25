import androidx.compose.runtime.Composable

@Composable
fun testInline_M1_W_Return_Func(condition: Boolean) {
    A()
    M3 {
        A()
        if (condition) {
            return@M3
        }
        A()
    }
    M3 {
        A()
        if (condition) {
            return@M3
        }
        A()
    }
    A()
}
