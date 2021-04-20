@file:JvmName("Utils")
@file:JvmMultifileClass

package declaration

fun foo(): Int = 42

fun String.buzz(): String {
    return "$this... zzz..."
}
