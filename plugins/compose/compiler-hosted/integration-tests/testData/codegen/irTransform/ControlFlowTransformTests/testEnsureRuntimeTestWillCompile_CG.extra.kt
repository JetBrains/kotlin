import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable

@Composable
inline fun InlineWrapper(content: @Composable () -> Unit) = content()

@Composable
inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

@Composable @NonRestartableComposable
fun Text(value: String) { }
