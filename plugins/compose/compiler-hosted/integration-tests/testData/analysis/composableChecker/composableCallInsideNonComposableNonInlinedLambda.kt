// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

@Composable fun ComposableFunction() {}

fun functionThatTakesALambda(content: () -> Unit) { content() }

fun NonComposableFunction() {
    functionThatTakesALambda {
        <!COMPOSABLE_INVOCATION!>ComposableFunction<!>()  // invocation
    }
}
