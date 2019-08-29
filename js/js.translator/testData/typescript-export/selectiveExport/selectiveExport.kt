// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS

// FILE: file1.kt
package foo

@JsExport
val exportedVal = 10

@JsExport
fun exportedFun() = 10

@JsExport
class ExportedClass {
    val value = 10
}

@JsExport
external interface ExportedInternalInterface

val _val = 10

fun _fun() = 10

class Class

external interface ExternalInterface

// FILE: file2.kt

@file:JsExport

package foo

val fileLevelExportedVal = 10
fun fileLevelExportedFun() = 10
class FileLevelExportedClass {
    val value = 10
}
external interface FileLevelExportedExternalInterface


