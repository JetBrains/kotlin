// "Import" "true"
// ERROR: Unresolved reference: someFun
package testingExtensionFunctionsImport

import testingExtensionFunctionsImport.data.someFun

fun String.some() {
    <caret>someFun()
}
