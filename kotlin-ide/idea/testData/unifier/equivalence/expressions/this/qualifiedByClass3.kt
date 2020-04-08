class A {
    inner class B {
        inner class C {
            fun foo() {
                <selection>this</selection>
                this@A
                this@B
                this@C
            }
        }

        fun foo() {
            this
            this@A
            this@B
        }
    }

    fun foo() {
        this
        this@A
    }
}