import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue

@Composable fun A() {
    val x by mutableStateOf("abc")
    B {
        print(x)
    }
}
