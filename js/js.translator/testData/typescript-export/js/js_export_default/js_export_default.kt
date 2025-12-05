// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// SPLIT_PER_MODULE
// MODULE: lib
// MODULE_KIND: ES
// ES_MODULES
// FILE: js-export-default.kt

package foo

@JsExport
fun getParent(): Parent {
    return Parent
}

@JsExport.Default
object Parent {
    object Nested1 {
        val value: String = "Nested1"
        class Nested2 {
            companion object {
                class Nested3
            }
        }
    }
}
