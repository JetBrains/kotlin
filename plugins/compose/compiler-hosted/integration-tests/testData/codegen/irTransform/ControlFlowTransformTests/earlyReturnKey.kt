import androidx.compose.runtime.*

@Composable
fun Test() {
    key(1) {
        return
        Test()
    }
}
