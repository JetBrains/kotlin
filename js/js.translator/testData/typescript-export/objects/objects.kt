// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: objects.kt

package foo

@JsExport
object O0

@JsExport
object O {
    val x = 10
    fun foo() = 20
}

@JsExport
fun takesO(o: O): Int =
    O.x + O.foo()

@JsExport
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

@JsExport
fun getParent(): Parent {
    return Parent
}

@JsExport
fun createNested1(): Parent.Nested1 {
    return Parent.Nested1
}

@JsExport
fun createNested2(): Parent.Nested1.Nested2 {
    return Parent.Nested1.Nested2()
}

@JsExport
fun createNested3(): Parent.Nested1.Nested2.Companion.Nested3 {
    return Parent.Nested1.Nested2.Companion.Nested3()
}