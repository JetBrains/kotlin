package b

import a.A

open class B : A() {
    fun t() {
        super.foo()
    }
}