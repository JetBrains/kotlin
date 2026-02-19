// DUMP_IR

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: PopupRenderer.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.unit.dp

interface PopupRenderer {
    // Notice that the declaration with default parameters is crashing the plugin
    // While the one without default parameters works fine
    @Composable
    fun Popup(
        onDismissRequest: (() -> Unit)? = null,
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
        onKeyEvent: ((KeyEvent) -> Boolean)? = null,
        content: @Composable () -> Unit,
    )
}

private object DefaultPopupRenderer : PopupRenderer {
    @Composable
    override fun Popup(
        onDismissRequest: (() -> Unit)?,
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?,
        content: @Composable () -> Unit,
    ) {
    }
}

val LocalPopupRenderer: ProvidableCompositionLocal<PopupRenderer> =
    staticCompositionLocalOf { DefaultPopupRenderer }

// MODULE: main(lib)
// FILE: main.kt
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.foundation.text.BasicText

private object AlternativeDesktopPopupRenderer : PopupRenderer {
    @Composable
    override fun Popup(
        onDismissRequest: (() -> Unit)?,
        onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
        onKeyEvent: ((KeyEvent) -> Boolean)?,
        content: @Composable () -> Unit,
    ) {
       BasicText("Test")
    }
}