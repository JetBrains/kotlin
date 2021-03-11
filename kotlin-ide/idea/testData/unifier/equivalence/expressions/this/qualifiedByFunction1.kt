class A

fun A.foo() {
    fun A.bar() {
        fun A.baz() {
            this
            this@baz
            this@bar
            this@foo
        }

        this
        this@bar
        this@foo
    }

    this
    <selection>this@foo</selection>
}