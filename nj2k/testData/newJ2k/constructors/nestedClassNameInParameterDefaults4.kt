package pack

import pack.A.Nested

internal class A @JvmOverloads constructor(nested: Nested? = Nested(Nested.FIELD)) {
    internal class Nested(p: Int) {
        companion object {
            const val FIELD = 0
        }
    }
}

internal class B {
    var nested: Nested? = null
}