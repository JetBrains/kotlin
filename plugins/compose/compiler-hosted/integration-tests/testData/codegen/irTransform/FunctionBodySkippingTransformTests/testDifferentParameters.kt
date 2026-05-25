import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


val fooGlobal = 10

@Composable
fun A(x: Int) {
    B(
        // direct parameter
        x,
        // transformation
        x + 1,
        // literal
        123,
        // expression with no parameter
        fooGlobal
    )
}
