// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_MINIFICATION
// SKIP_NODE_JS
// MODULE_KIND: UMD

@file:JsExport

package foo

val prop = 10

class C(val x: Int) {
    fun doubleX() = x * 2
}

fun box(): String = "OK"