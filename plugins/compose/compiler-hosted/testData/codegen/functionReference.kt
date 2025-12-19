// DUMP_IR

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: items.kt

import androidx.compose.runtime.Composable

@Composable fun Test() {}
@Composable fun TestWithParams(param: Int) {}

// MODULE: main(ui)
import androidx.compose.runtime.Composable

val x = ::Test
val x1 = ::TestWithParams

@Composable fun App() {
    x()
    x1(0)
}