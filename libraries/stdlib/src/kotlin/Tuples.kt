package kotlin

import kotlin.nullable.hashCodeOrDefault
import java.io.Serializable

// TODO: make it a data class
public class Pair<A, B> (
        public val first: A,
        public val second: B
) : Serializable {
    public fun component1(): A = first
    public fun component2(): B = second

    override fun toString(): String = "($first, $second)"

    override fun hashCode(): Int {
        var result = first.hashCodeOrDefault(0)
        result = 31 * result + second.hashCodeOrDefault(0)
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
        var result = first.hashCodeOrDefault(0)
        result = 31 * result + second.hashCodeOrDefault(0)
        result = 31 * result + third.hashCodeOrDefault(0)
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