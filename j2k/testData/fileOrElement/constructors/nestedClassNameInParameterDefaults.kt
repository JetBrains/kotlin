internal class A @JvmOverloads constructor(nested: A.Nested = A.Nested(A.Nested.FIELD)) {

    internal class Nested(p: Int) {
        companion object {

            val FIELD = 0
        }
    }
}