class A(nested: A.Nested = A.Nested(A.Nested.FIELD)) {

    class Nested(p: Int) {
        class object {

            public val FIELD: Int = 0
        }
    }
}