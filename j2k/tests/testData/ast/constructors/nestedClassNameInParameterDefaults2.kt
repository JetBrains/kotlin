import A.Nested

class A(nested: Nested = Nested(Nested.FIELD)) {

    class Nested(p: Int) {
        class object {

            public val FIELD: Int = 0
        }
    }
}

class B {
    var nested: Nested
}