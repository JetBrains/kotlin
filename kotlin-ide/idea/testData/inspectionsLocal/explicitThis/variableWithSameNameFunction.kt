class Foo {
    fun foo() {
        fun a() {
        }
        <caret>this.a
    }

    val a = 1
}
