// "Safe delete 'foo'" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.UnusedSymbolInspection

actual class My {
    actual fun <caret>foo() {}
}