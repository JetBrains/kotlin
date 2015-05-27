class A @jvmOverloads constructor(nested: A.Nested = A.Nested(A.Nested.FIELD)) {

    class Nested(p: Int) {
        companion object {

            public val FIELD: Int = 0
        }
    }
}