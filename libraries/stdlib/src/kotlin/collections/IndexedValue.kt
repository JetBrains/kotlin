package kotlin.collections

/**
 * Data class representing a value from a collection or sequence, along with its index in that collection or sequence.
 *
 * @property value the underlying value.
 * @property index the index of the value in the collection or sequence.
 */
public data class IndexedValue<out T>(public val index: Int, public val value: T)
