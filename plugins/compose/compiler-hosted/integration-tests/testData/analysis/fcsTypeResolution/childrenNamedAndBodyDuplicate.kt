// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable fun A(content: @Composable () -> Unit) { content() }

@Composable fun Test() {
    A(content={}) <!TOO_MANY_ARGUMENTS!>{ }<!>
}
