package test2

import test.BaseOtherPackage

class DerivedOtherPackage protected constructor() : BaseOtherPackage() {
    init {
        foo()
        val i = i
    }
}