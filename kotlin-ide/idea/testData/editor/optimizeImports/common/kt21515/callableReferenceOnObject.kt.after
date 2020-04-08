package foo

import foo.CallableReferenceOnObject.Base.Companion.FromBaseCompanion
import foo.CallableReferenceOnObject.CompanionSupertype.FromCompanionSupertype

object CallableReferenceOnObject {

    open class Base {
        companion object {
            object FromBaseCompanion {
                fun foo() = 42
            }
        }
    }

    open class CompanionSupertype {
        object FromCompanionSupertype {
            fun foo() = 42
        }
    }
}

class Derived : CallableReferenceOnObject.Base() {
    companion object : CallableReferenceOnObject.CompanionSupertype() { }

    // Callable references
    val c = FromBaseCompanion::foo
    val d = FromCompanionSupertype::foo
}