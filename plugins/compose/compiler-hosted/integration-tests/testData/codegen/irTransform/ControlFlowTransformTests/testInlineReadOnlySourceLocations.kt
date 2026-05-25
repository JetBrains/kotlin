import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

val current
    @Composable
    @ReadOnlyComposable
    get() = 0

@Composable
@ReadOnlyComposable
fun calculateSometing(): Int {
    return 0;
}

@Composable
fun Test() {
    val c = current
    val cl = calculateSometing()
    Layout {
        Text("$c $cl")
    }
}
