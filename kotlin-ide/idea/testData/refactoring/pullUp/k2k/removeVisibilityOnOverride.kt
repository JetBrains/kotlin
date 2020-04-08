open class A {

}

open class <caret>B : A() {
    // INFO: {"checked": "true", "toAbstract": "true"}
    protected fun foo() {}
}