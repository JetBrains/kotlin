import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.MutableState

@Composable
fun VarargComposable(state: MutableState<Int>, vararg values: String = Array(1) { "value " + it }) {
    state.value
}
