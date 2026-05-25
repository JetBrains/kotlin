import androidx.compose.runtime.Composable
    import androidx.compose.runtime.remember

    
@Composable
fun Test(a: Boolean, visible: Boolean, onDismiss: () -> Unit) {
    if (a) {
        val a = someComposableValue()
        used(a)
        val m = Modifier()
        val dismissModifier = if (visible) {
            m.pointerInput(Unit) { detectTapGestures { onDismiss() } }
        } else {
            m
        }
        used(dismissModifier)
    }
}
