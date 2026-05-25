import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.*

@Composable
@ExplicitGroupsComposable
inline fun ReusableContentHost(active: Boolean, crossinline content: @Composable () -> Unit) {
    currentComposer.startReusableGroup(200, active)
    val activeChanged = currentComposer.changed(active)
    if (active) {
        content()
    } else {
        currentComposer.deactivateToEndGroup(activeChanged)
    }
    currentComposer.endReusableGroup()
}
