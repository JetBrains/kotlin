package kotlin

/**
 * Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
 * represented as values of the primitive type `boolean`.
 */
public class Boolean : Comparable<Boolean> {
    /**
     * Returns the inverse of this boolean.
     */
    external public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one.
     */
    external public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one.
     */
    external public infix fun or(other: Boolean): Boolean

    /**
     * Performs a logical `xor` operation between this Boolean and the [other] one.
     */
    external public infix fun xor(other: Boolean): Boolean

    external public override fun compareTo(other: Boolean): Int

    // Konan-specific.
    @SymbolName("Kotlin_Boolean_toString")
    external public override fun toString(): String
}