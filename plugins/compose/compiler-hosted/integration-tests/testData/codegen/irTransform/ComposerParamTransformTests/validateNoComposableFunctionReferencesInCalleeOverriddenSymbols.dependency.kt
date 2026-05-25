package dependency

import androidx.compose.runtime.Composable

interface Content {
    fun setContent(c: @Composable () -> Unit = {})
}
class ContentImpl : Content {
    override fun setContent(c: @Composable () -> Unit) {}
}
