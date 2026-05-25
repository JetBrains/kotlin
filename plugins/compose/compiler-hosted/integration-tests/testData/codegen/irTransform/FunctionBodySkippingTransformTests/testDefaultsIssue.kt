import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun Box2(
    modifier: Modifier = Modifier,
    paddingStart: Dp = Dp.Unspecified,
    content: @Composable () -> Unit = {}
) {
    used(modifier)
    used(paddingStart)
    content()
}
