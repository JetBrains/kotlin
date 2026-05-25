import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


fun A(factory: @Composable () -> Int): Unit {}
fun B() = A { 123 }
