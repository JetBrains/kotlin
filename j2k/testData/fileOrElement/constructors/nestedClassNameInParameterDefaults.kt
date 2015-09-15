internal class A JvmOverloads internal constructor(nested: A.Nested = A.Nested(A.Nested.FIELD)) {

    internal class Nested internal constructor(p: Int) {
        companion object {

            val FIELD = 0
        }
    }
}