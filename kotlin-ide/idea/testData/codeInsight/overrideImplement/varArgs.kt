open class S {
    open fun s(vararg v: Int) {}
}

class D : S() {
    <caret>
}
