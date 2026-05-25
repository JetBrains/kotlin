import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun A(x: Int) {
    Provide { y ->
        Provide { z ->
            B(x, y, z)
        }
        B(x, y)
    }
    B(x)
}
