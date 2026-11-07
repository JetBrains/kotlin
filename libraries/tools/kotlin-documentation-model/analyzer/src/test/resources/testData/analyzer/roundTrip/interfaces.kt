// MODULE: main
// FILE: example/Api.kt
package example

interface Api<T : CharSequence> {
    context(label: String)
    fun <R> convert(value: T, fallback: R? = null): R

    context(key: String, _: Int)
    val items: Map<String, List<T?>>

    interface Nested {
        val enabled: Boolean
    }

    var selected: T?

    suspend fun update(value: T, enabled: Boolean = true, tags: List<String> = emptyList()): Unit
}

typealias Names = List<String?>
