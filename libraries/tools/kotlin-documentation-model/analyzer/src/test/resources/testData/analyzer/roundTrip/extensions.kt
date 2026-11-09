// MODULE: main
// FILE: example/extensions.kt
package example

fun <T> List<T>.copyItems(): List<T>

interface Extensions<T> {
    fun String.append(value: T): String

    val List<T>.firstItem: T?

    var String.label: String

    fun T.transform(): T
}

val <T> List<T>.head: T?

fun String?.orDefault(fallback: String = ""): String

context(prefix: String)
fun String.prefixed(): String

suspend fun String.refresh(): String

val String.size: Int
