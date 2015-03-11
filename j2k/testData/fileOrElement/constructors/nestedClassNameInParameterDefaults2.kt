// ERROR: Property must be initialized or be abstract
import A.Nested

class A(nested: Nested = Nested(Nested.FIELD)) {

    class Nested(p: Int) {
        default object {

            public val FIELD: Int = 0
        }
    }
}

class B {
    var nested: Nested
}