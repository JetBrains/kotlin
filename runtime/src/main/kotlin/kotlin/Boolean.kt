package kotlin

/**
 * Represents a value which is either `true` or `false`. On the JVM, non-nullable values of this type are
 * represented as values of the primitive type `boolean`.
 */
public final class Boolean : Comparable<Boolean> {
    /**
     * Returns the inverse of this boolean.
     */
    @SymbolName("Kotlin_Boolean_not")
    external public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_and_Boolean")
    external public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_or_Boolean")
    external public infix fun or(other: Boolean): Boolean

    /**
     * Performs a logical `xor` operation between this Boolean and the [other] one.
     */
    @SymbolName("Kotlin_Boolean_xor_Boolean")
    external public infix fun xor(other: Boolean): Boolean

    @SymbolName("Kotlin_Boolean_compareTo_Boolean")
    external public override fun compareTo(other: Boolean): Int

    // Konan-specific.
    public fun equals(other: Boolean): Boolean = konan.internal.areEqualByValue(this, other)

    public override fun equals(other: Any?): Boolean =
        other is Boolean && konan.internal.areEqualByValue(this, other)

    @SymbolName("Kotlin_Boolean_toString")
    external public override fun toString(): String

    public override fun hashCode(): Int {
        return if (this) 1 else 0;
    }
}