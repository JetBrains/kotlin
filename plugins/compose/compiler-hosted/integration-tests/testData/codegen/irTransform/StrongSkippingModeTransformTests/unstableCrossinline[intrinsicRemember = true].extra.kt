import androidx.compose.runtime.Composable


@Composable
fun Dialog(content: (@Composable () -> Unit)?) { content?.invoke() }

@Composable
fun Button(
    onClick: () -> Unit
) {}

inline fun <ValueT : Any> slotIfNotNull(
    value: ValueT?,
    crossinline slot: @Composable (ValueT) -> Unit
): (@Composable () -> Unit)? {
    return if (value != null) {
        @Composable { slot(value) }
    } else null
}

fun used(x: Any?) {}
