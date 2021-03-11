class A {
    fun bar() {

    }
}

fun A.foo() {
    <selection>bar()</selection>
    this.bar()
    this@foo.bar()
}