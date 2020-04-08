class A

fun A.foo() {
    fun A.bar() {
        fun A.baz() {
            this
            <selection>this@baz</selection>
            this@bar
            this@foo
        }

        this
        <selection>this@bar</selection>
        this@foo
    }

    this
    this@foo
}