// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable
fun Int.Foo(content: @Composable Int.(foo: String) -> Unit) {
    <!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>content<!>()
}

@Composable
fun Bar(content: @Composable Int.() -> Unit) {
    <!NO_VALUE_FOR_PARAMETER!>content<!>()
}
