import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

class CurrentHolder {
    inline val current: Int
        @ReadOnlyComposable
        @Composable
        get() = 0
}

class HolderHolder {
    private val _currentHolder = CurrentHolder()
    val current: Int
        @ReadOnlyComposable
        @Composable
        get() = _currentHolder.current
}

val holderHolder = HolderHolder()

@Composable
@ReadOnlyComposable
fun calculateSomething(): Int {
    return 0;
}

@Composable
fun Test() {
    val c = holderHolder.current
    val cl = calculateSomething()
    Layout {
        Text("$c $cl")
    }
}
