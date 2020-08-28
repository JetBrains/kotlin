class TestLambda<T> {
    class Box<Q>(private val value: Q) {
        fun unbox(): Q {
            return value
        }
    }

    fun toStringAllBox(list: List<Box<T>>): String {
        return list.stream().map { obj: Box<T> -> obj.unbox() }.map { obj: T -> obj.toString() }
            .reduce { s1: String, s2: String -> "$s1, $s2" }.orElse("")
    }
}