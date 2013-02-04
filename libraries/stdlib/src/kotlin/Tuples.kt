package kotlin

import java.io.Serializable

private fun Any?.safeHashCode() : Int = if (this == null) 0 else this.hashCode()

// TODO: make it a data class
public class Pair<out A, out B> (
        public val first: A,
        public val second: B
) : Serializable {
    public fun component1(): A = first
    public fun component2(): B = second

    override fun toString(): String = "($first, $second)"

    override fun hashCode(): Int {
        var result = first.safeHashCode()
        result = 31 * result + second.safeHashCode()
        return result;
    }

    override fun equals(o: Any?): Boolean {
        if (this identityEquals o) return true;
        if (o == null || this.javaClass != o.javaClass) return false;

        val t = o as Pair<*, *>
        return first == t.first && second == t.second;
    }
}

public class Triple<A, B, C> (
        public val first: A,
        public val second: B,
        public val third: C
) : Serializable {
    public fun component1(): A = first
    public fun component2(): B = second
    public fun component3(): C = third

    override fun toString(): String = "($first, $second, $third)"

    override fun hashCode(): Int {
        var result = first.safeHashCode()
        result = 31 * result + second.safeHashCode()
        result = 31 * result + third.safeHashCode()
        return result;
    }

    override fun equals(o: Any?): Boolean {
        if (this identityEquals o) return true;
        if (o == null || this.javaClass != o.javaClass) return false;

        val t = o as Triple<*, *, *>
        return first == t.first &&
               second == t.second &&
               third == t.third;
    }
}
