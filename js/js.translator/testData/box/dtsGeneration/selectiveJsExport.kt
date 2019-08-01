// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS

// FILE: file1.kt
package foo

fun box(): String = "OK"

@JsExport
val exportedVal = 10

@JsExport
fun exportedFun() = 10

@JsExport
class ExportedClass

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
class FileLevelExportedClass
external interface FileLevelExportedExternalInterface


