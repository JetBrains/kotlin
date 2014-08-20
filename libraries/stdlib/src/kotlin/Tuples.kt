package kotlin

import java.io.Serializable

public data class Pair<out A, out B>(
        public val first: A,
        public val second: B
) : Serializable {
    override fun toString(): String = "($first, $second)"
}

public data class Triple<out A, out B, out C>(
        public val first: A,
        public val second: B,
        public val third: C
) : Serializable {
    override fun toString(): String = "($first, $second, $third)"
}
