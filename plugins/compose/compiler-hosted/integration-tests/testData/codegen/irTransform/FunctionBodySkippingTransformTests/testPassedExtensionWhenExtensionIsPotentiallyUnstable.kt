import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable fun Unstable.Test() {
    doSomething(this) // does this reference %dirty without %dirty
}

@Composable fun doSomething(x: Unstable) {}
