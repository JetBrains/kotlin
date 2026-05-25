import androidx.compose.runtime.*

fun interface MeasurePolicy {
    fun invoke(size: Int)
}
@Composable inline fun Layout(content: @Composable () -> Unit) {}
@Composable fun Box() {}
