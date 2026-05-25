// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun TextView(text: String) { print(text) }

@Composable fun doSomething(foo: Boolean) {
    when (foo) {
        true -> TextView(text="Started...")
        false -> TextView(text="Continue...")
    }
}
