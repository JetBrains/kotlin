import androidx.compose.runtime.Composable

@Composable
fun test_CM1_RetFun(condition: Boolean) {
    Text("Root - before")
    M1 {
        Text("M1 - before")
        if (condition) return
        Text("M1 - after")
    }
    Text("Root - after")
}
