package foo

import foo.onlyInImport
import foo.onlyInImportNoWarn

fun onlyInImport() {

}

@Suppress("unused")
fun onlyInImportNoWarn() {}
