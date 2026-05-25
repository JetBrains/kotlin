import androidx.compose.runtime.Composable
import java.time.Instant

@Composable
fun SkippableComposable(list: List<String>, instant: Instant) {
    use(list)
    use(instant)
}
