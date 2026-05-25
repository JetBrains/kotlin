import androidx.compose.runtime.*

@ExplicitGroupsComposable
@Composable
inline fun Test(active: Boolean, content: @Composable () -> Unit) {
    currentComposer.startReusableGroup(1, null)
    if (active) {
        content()
    } else {
        currentComposer.deactivateToEndGroup(false)
    }
    currentComposer.endReusableGroup()
}
