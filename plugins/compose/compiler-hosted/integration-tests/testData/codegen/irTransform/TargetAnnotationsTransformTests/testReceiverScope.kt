import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.*

@Immutable
interface LocalBoxScope {
    @Stable
    fun Modifier.align(alignment: Alignment): Modifier
}

object LocalBoxScopeInstance : LocalBoxScope {
    override fun Modifier.align(alignment: Alignment): Modifier = Modifier
}

val localBoxMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(
        constraints.minWidth,
        constraints.minHeight
    ) {}
}

@Composable
inline fun LocalBox(
    modifier: Modifier = Modifier,
    content: @Composable LocalBoxScope.() -> Unit
) {
    Layout(
        modifier = modifier,
        measurePolicy = localBoxMeasurePolicy,
        content = { LocalBoxScopeInstance.content() }
    )
}
