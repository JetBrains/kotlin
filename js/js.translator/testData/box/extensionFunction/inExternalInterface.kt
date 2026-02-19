// LANGUAGE: +JsEnableExtensionFunctionInExternals

external interface Foo {
    fun foo(): String.() -> Unit
}

fun box(): String {
    return "OK"
}