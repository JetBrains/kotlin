// ERROR: Property must be initialized or be abstract
import A.Nested

class A jvmOverloads constructor(nested: Nested = Nested(Nested.FIELD)) {

    class Nested(p: Int) {
        companion object {

            public val FIELD: Int = 0
        }
    }
}

class B {
    var nested: Nested
}