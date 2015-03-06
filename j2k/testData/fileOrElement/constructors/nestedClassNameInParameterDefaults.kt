class A(nested: A.Nested = A.Nested(A.Nested.FIELD)) {

    class Nested(p: Int) {
        default object {

            public val FIELD: Int = 0
        }
    }
}