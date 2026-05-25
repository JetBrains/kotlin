// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

typealias COMPOSABLE_UNIT_LAMBDA = @Composable () -> Unit

@Composable
fun ComposeWrapperComposable(content: COMPOSABLE_UNIT_LAMBDA) {
    MyComposeWrapper {
        content()
    }
}

@Composable fun MyComposeWrapper(content: COMPOSABLE_UNIT_LAMBDA) {
    print(content.hashCode())
}
