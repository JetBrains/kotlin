import androidx.compose.runtime.Composable

@Composable
fun Test_M3_M1_Return_M3(condition: Boolean) {
    A()
    M3 {
        A()
        M1 {
            if (condition) {
                return@M3
            }
        }
        A()
    }
    A()
}
