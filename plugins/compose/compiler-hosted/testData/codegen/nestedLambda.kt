// DUMP_IR

// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class Color(val r: Int, val g: Int, val b:Int)

@Composable
fun rememberDominantColorState(defaultColor: Color = Color(0, 0, 0)): DominantColorState = remember {
    DominantColorState(defaultColor)
}

@Composable
fun DynamicThemePrimaryColorsFromImage(
    dominantColorState: DominantColorState = rememberDominantColorState(),
    content: @Composable () -> Unit
) {
}

@Stable
class DominantColorState(private val defaultColor: Color) {
    var color by mutableStateOf(defaultColor)
        private set
}

interface ColumnScope {
}

@Composable
inline fun Column(content: @Composable ColumnScope.() -> Unit) {
}

@Composable
fun Home() {
    Column {
        val dominantColorState = rememberDominantColorState()
        DynamicThemePrimaryColorsFromImage(dominantColorState) {
        }
    }
}