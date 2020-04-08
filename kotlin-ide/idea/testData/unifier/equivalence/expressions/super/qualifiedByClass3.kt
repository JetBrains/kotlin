open class Z {
    open fun foo() {

    }
}

class A: Z() {
    inner class B: Z() {
        inner class C: Z() {
            override fun foo() {
                super.foo()
                super@A.foo()
                super@B.foo()
                <selection>super@C.foo()</selection>
                super<Z>.foo()
                super<Z>@A.foo()
                super<Z>@B.foo()
                super<Z>@C.foo()
            }
        }

        override fun foo() {
            super.foo()
            super@A.foo()
            super@B.foo()
            super<Z>.foo()
            super<Z>@A.foo()
            super<Z>@B.foo()
        }
    }

    override fun foo() {
        super.foo()
        super@A.foo()
        super<Z>.foo()
        super<Z>@A.foo()
    }
}