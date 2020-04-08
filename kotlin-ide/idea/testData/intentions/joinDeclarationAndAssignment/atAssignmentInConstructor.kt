class A {
    constructor() {
        val foo: String<caret>
        bar()
        foo = ""
    }

    fun bar() {}
}
