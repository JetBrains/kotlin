// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable @ComposableTarget("Ui") fun Ui() {}

interface View {
    fun setContent(content: @Composable () -> Unit) {}
}

@Composable
@ComposableTarget("NotUi")
fun Test(view: View) {
    view.setContent { Ui() }
}
