// "Specify supertype" "true"
interface X {}

open class Y<T> {
    open fun foo() {}
}

interface Z {
    fun foo() {}
}

class Test : Y<Int>(), X, Z {
    override fun foo() {
        <caret>super.foo()
    }
}