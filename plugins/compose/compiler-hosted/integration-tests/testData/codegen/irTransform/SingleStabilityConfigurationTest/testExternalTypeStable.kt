import androidx.compose.runtime.Composable
import java.time.Instant

@Composable
fun SkippableComposable(list: List<String>) {
    use(list)
}

@Composable
fun UnskippableComposable(instant: Instant) {
    use(instant)
}
