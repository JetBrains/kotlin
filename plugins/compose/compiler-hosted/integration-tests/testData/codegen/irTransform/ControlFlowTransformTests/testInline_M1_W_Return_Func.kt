import androidx.compose.runtime.Composable

@Composable
fun testInline_M1_W_Return_Func(condition: Boolean) {
    A()
    M1 {
        A()
        while(true) {
            A()
            if (condition) {
                return
            }
            A()
        }
        A()
    }
    A()
}
