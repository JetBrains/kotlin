// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// MODULE: JS_TESTS
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

@JsExport.Ignore
val _val = 10

@JsExport.Ignore
fun _fun() = 10

@JsExport.Ignore
class Class

@JsExport.Ignore
external interface ExternalInterface

@JsExport.Ignore
@JsName("bbb")
fun zzz(x: Int = 10) {}
// FILE: file2.kt

<!REPEATED_ANNOTATION!>@file:JsExport<!>

package foo

val fileLevelExportedVal = 10
fun fileLevelExportedFun() = 10
class FileLevelExportedClass {
    val value = 10
}
external interface FileLevelExportedExternalInterface
