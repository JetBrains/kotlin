package test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import dependency.ContentImpl

@Composable
@NonRestartableComposable
fun Foo() {
    ContentImpl().setContent()
}
