import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun Bar.CanSkip(b: Foo = Foo()) {
    print("Hello World")
}
