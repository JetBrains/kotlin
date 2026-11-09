// MODULE: main
// FILE: example/Types.kt
package example

class Types<T : CharSequence>(val value: T) {
    val function: (T) -> String = { it.toString() }
    val nullableFunction: ((T) -> String)? = null
    val extensionFunction: String.(T) -> Int = { length }
    val suspendFunction: suspend (T) -> Unit = {}
    fun <R : Any> convert(input: R?, block: (T) -> R): R = block(value)
    fun String.extension(other: T): String = this + other
}
