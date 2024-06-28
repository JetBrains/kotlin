import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.DomApplier

@OptIn(ComposeWebInternalApi::class)
fun main() {
    DomApplier::class.js
}