// "Import" "true"
// ERROR: Unresolved reference: someFun
package testingExtensionFunctionsImport

fun String.some() {
    <caret>someFun()
}
