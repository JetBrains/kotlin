// ERROR: Property must be initialized or be abstract
package pack

import pack.A.Nested

internal class A JvmOverloads internal constructor(nested: Nested = Nested(Nested.FIELD)) {

    internal class Nested internal constructor(p: Int) {
        companion object {

            val FIELD: Int = 0
        }
    }
}

internal class B {
    internal var nested: Nested
}