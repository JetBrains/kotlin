data class Pair<out A, out B>(
        public val first: A,
        public val second: B
)


public fun <T> listOf(): List<T> = throw Error()
