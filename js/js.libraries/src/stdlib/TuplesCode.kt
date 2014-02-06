package kotlin

// TODO: make it a data class
public class Pair<A, B> (
        public val first: A,
        public val second: B
) {
    public fun component1(): A = first
    public fun component2(): B = second

    override fun toString(): String = "($first, $second)"
}

public class Triple<A, B, C> (
        public val first: A,
        public val second: B,
        public val third: C
) {
    public fun component1(): A = first
    public fun component2(): B = second
    public fun component3(): C = third

    override fun toString(): String = "($first, $second, $third)"
}