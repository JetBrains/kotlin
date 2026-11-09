// MODULE: main
// FILE: example/Types.kt
package example

interface Base<T>

class Types<T : CharSequence>(val value: T) : Base<List<T>> {
    val nullable: String? = null
    val nested: Map<String, List<T?>> = emptyMap()
    val projections: Map<in String, out List<*>> = emptyMap<String, List<*>>()
}

interface MultipleBounds<T> where T : CharSequence, T : Comparable<T> {
    fun <R> transform(value: R): T where R : CharSequence, R : Comparable<R>
}
