import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableTarget
import androidx.compose.runtime.ComposableOpenTarget
import androidx.compose.runtime.Applier

@Composable
@ComposableTarget("UI")
fun Layout() { }

@Composable
@ComposableTarget("UI")
fun Layout(content: @Composable @ComposableTarget("UI") () -> Unit) { }

@Composable
@ComposableTarget("UI")
inline fun InlineLayout(content: @Composable @ComposableTarget("UI") () -> Unit) { }


@Composable
fun Text(text: String) { Layout() }

@Composable
fun Row(content: @Composable () -> Unit) {
    Layout(content)
}

@Composable
inline fun InlineRow(content: @Composable () -> Unit) {
    InlineLayout(content)
}

@Composable
@ComposableTarget("Vector")
fun Vector() { }

@Composable
fun Circle() { Vector() }

@Composable
fun Square() { Vector() }

@Composable
@ComposableTarget("Vector")
fun Vector(content: @Composable @ComposableTarget("Vector") () -> Unit) { }

@Composable
fun Layer(content: @Composable () -> Unit) { Vector(content) }

@Composable
@ComposableTarget("UI")
fun Drawing(content: @Composable @ComposableTarget("Vector") () -> Unit) { }

@Composable
fun Wrapper(content: @Composable () -> Unit) { content() }
