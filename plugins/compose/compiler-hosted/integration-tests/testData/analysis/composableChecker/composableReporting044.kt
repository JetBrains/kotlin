// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

typealias UNIT_LAMBDA = () -> Unit

@Composable
fun FancyButton() {}

@Composable
fun Noise() {
    FancyButton()
}
