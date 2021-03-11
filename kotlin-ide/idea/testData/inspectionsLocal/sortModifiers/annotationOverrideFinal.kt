annotation class Ann

open class Base {
    open fun bar() {}
}

open class Test : Base() {
    @Ann
    <caret>override final fun bar() {}
}