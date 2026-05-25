package test

import androidx.compose.runtime.Composable
import dependency.Content

class ContentImpl : Content {
    override fun setContent(c: @Composable () -> Unit) {}
}
