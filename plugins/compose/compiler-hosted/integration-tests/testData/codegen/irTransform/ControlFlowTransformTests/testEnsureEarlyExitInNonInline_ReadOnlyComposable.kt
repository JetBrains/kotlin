import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.NonRestartableComposable


import androidx.compose.runtime.currentComposer

object obj {
    val condition = false
}

@Composable
@ReadOnlyComposable
fun Calculate(condition: Boolean): Boolean {
    if (condition) return false

    with (obj) {
        if (condition) return false
        return currentComposer.inserting
    }
}
