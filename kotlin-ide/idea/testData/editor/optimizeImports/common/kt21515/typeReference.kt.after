package foo

import foo.TypeReference.Base.Companion.FromBaseCompanion
import foo.TypeReference.CompanionSupertype.FromCompanionSupertype

object TypeReference {

    open class Base {
        companion object {
            class FromBaseCompanion {
                fun foo() = 42
            }
        }
    }

    open class CompanionSupertype {
        class FromCompanionSupertype {
            fun foo() = 42
        }
    }
}

class Derived : TypeReference.Base() {
    companion object : TypeReference.CompanionSupertype() {
    }

    // Type references
    val a: FromBaseCompanion? = null
    val b: FromCompanionSupertype? = null
}
