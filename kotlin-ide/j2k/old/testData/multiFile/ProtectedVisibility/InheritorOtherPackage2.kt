package test2

import test.*

class DerivedOtherPackage protected constructor() : BaseOtherPackage() {
    init {
        foo()
        val i = this.i
    }
}