// ISSUE: KT-91194
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// MODULE: JS_TESTS
// FILE: abstract-suspend-interface-override.kt

package foo

@JsExport
interface ValueProvider {
    suspend fun value(x: Int = 7): String
}

@JsExport
abstract class Base : ValueProvider {
    abstract override suspend fun value(x: Int): String
}

@JsExport
class Implementation : Base() {
    override suspend fun value(x: Int): String = "K" + x
}
