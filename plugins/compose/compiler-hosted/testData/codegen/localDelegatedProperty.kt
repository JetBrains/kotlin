// DUMP_IR
// DUMP_INVOKED_METHODS

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: p2/P.kt
package p2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState

@Composable
fun <T> P.delegate(initialValue: T): State<T> {
    return produceState(initialValue) {
        value = initialValue
    }
}

class P

// MODULE: testMain(lib)
// FILE: HomeViewModel.kt
package home

import p2.P

internal class HomeViewModel(val value: String) {
    internal val uiState: P = P()
}

// FILE: main.kt
package home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import home.HomeViewModel
import p2.delegate

@Composable
internal fun test(homeViewModel: HomeViewModel): String {
    val bar by homeViewModel.uiState.delegate("a")
    return bar
}