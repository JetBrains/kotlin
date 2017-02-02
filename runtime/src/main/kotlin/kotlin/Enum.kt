package kotlin

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public abstract class Enum<E: Enum<E>>(public val name: String, public val ordinal: Int): Comparable<E> {

    public override final fun compareTo(other: E): Int { return ordinal - other.ordinal }

    /**
     * Throws an exception since enum constants cannot be cloned.
     * This method prevents enum classes from inheriting from [Cloneable].
     */
    protected final fun clone(): Any {
        throw UnsupportedOperationException()
    }

    public override final fun equals(other: Any?): Boolean {
        return other is Enum<*> && ordinal == other.ordinal
    }

    public override final fun hashCode(): Int {
        return ordinal
    }

    public override fun toString(): String {
        return name
    }
}
