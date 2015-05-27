// ERROR: Property must be initialized or be abstract
package pack

import pack.A.Nested

class A @jvmOverloads constructor(nested: Nested = Nested(Nested.FIELD)) {

    class Nested(p: Int) {
        companion object {

            public val FIELD: Int = 0
        }
    }
}

class B {
    var nested: Nested
}