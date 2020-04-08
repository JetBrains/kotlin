class A {
    inner class B {
        inner class C {
            fun foo() {
                this
                this@A
                this@B
                this@C
            }
        }

        fun foo() {
            this
            <selection>this@A</selection>
            this@B
        }
    }

    fun foo() {
        this
        this@A
    }
}