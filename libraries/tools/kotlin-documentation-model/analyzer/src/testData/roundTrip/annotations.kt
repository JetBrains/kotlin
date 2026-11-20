// MODULE: main
// FILE: example/Annotations.kt
package example

@Deprecated(message = "Use Replacement")
interface Annotated {
    @SinceKotlin(version = "1.1")
    fun call(value: String): Unit

    @Deprecated(message = "Use call", replaceWith = ReplaceWith(expression = "call(value)", imports = ["example.Annotated"]), level = DeprecationLevel.WARNING)
    fun old(value: String): Unit

    @Deprecated(message = "Use call")
    val property: String
}

annotation class Label(val value: String)
