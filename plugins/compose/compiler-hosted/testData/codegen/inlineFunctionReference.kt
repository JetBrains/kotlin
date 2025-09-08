// DUMP_IR

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: items.kt

import androidx.compose.runtime.Composable

inline fun items(crossinline content: @Composable () -> Unit) {}

// MODULE: main(ui)
import androidx.compose.runtime.Composable

val test: (@Composable () -> Unit) -> Unit = ::items

val test1: (@Composable () -> Unit) -> Unit = { it::invoke }