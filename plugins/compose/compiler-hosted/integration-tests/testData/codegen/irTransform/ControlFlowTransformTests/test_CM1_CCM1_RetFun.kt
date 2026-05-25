import androidx.compose.runtime.Composable

@Composable
fun test_CM1_CCM1_RetFun(condition: Boolean) {
    Text("Root - before")
    M1 {
        Text("M1 - begin")
        if (condition) {
            Text("if - begin")
            M1 {
                Text("In CCM1")
                return@test_CM1_CCM1_RetFun
            }
        }
        Text("M1 - end")
    }
    Text("Root - end")
}
