@file:JvmName("Utils")
@file:JvmMultifileClass

package declaration

// Single-line comment bound to fun foo
fun foo(): Int = 42

/*
 * Multi-line comment bound to extension fun buzz
 */
fun String.buzz(): String {
    return "$this... zzz..."
}
