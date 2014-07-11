package pack

import pack.A.*

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