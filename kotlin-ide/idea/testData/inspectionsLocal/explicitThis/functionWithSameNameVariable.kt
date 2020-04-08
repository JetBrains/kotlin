class Foo {
    fun foo() {
        val a = 1
        <caret>this.a()
    }

    fun a() {
    }
}