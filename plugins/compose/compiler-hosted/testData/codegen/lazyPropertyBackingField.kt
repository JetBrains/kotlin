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
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import compose.ui.Color

@Composable
fun rememberDominantColorState(defaultColor: Color = Color(0, 0, 0)): DominantColorState = remember {
    DominantColorState(defaultColor)
}

@Composable
fun DynamicThemePrimaryColorsFromImage(
    dominantColorState: DominantColorState = rememberDominantColorState()
) {
}

@Stable
class DominantColorState(private val defaultColor: Color) {
    var color by mutableStateOf(defaultColor)
        private set
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
        DynamicThemePrimaryColorsFromImage(dominantColorState)
    }
}