// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: compose/ui/foo.kt
package compose.ui

import androidx.compose.runtime.Composable

class Color(val r: Int, val g: Int, val b:Int)

interface ColumnScope {
}

@Composable
inline fun Column(content: @Composable ColumnScope.() -> Unit) {
}

// MODULE: main(lib)
// FILE: util/DominantColorState.kt
package util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import compose.ui.Color

@Composable
fun rememberDominantColorState(defaultOnColor: Color = Color(0, 0, 0)): DominantColorState = remember {
    DominantColorState(defaultColor)
}

@Composable
fun DynamicThemePrimaryColorsFromImage(
    dominantColorState: DominantColorState = rememberDominantColorState(),
    content: @Composable () -> Unit
) {
}

@Stable
class DominantColorState(private val defaultOnColor: Color) {
    var color = mutableStateOf(defaultColor)
}

// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import compose.ui.Column
import util.DynamicThemePrimaryColorsFromImage
import util.rememberDominantColorState

@Composable
fun Home() {
    Column {
        val dominantColorState = rememberDominantColorState()
        DynamicThemePrimaryColorsFromImage(dominantColorState) {
        }
    }
}
