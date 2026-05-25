import androidx.compose.runtime.Composable

@Composable
fun Test_M3_M1_Return_M1(condition: Boolean) {
    A()
    M3 {
        A()
        M1 {
            if (condition) {
                return@M1
            }
        }
        A()
    }
    A()
}
