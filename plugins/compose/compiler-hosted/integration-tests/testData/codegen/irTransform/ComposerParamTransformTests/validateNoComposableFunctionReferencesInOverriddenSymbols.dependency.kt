package dependency

import androidx.compose.runtime.Composable

interface Content {
    fun setContent(c: @Composable () -> Unit)
}
