import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


object obj {
    val condition = true
}

@Composable
fun Test(condition: Boolean) {
    if (condition) return
    with (obj) {
        if (condition) return
    }
    A()
}
