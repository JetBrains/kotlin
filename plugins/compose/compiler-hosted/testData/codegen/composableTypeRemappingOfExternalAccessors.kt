// DUMP_IR

// This is a regression test against a bug that caused miscompilation of cross-module calls to
// getters and setters. For more details, see https://issuetracker.google.com/issues/537617330.

// MODULE: lib
import androidx.compose.runtime.Composable

var fakePrompt: (@Composable (
    onSuccess: () -> Unit,
    onError: () -> Unit,
    onCancel: () -> Unit,
    onUsePin: () -> Unit,
) -> Unit)? = null

// MODULE: main(lib)
import androidx.compose.runtime.Composable

fun main() {
    val fp = fakePrompt
    fakePrompt = { _, _, _, _ -> }
}
