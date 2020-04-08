package testing

interface Trait {
    open fun foo(a: Int, b: String) {
    }
}

open class Super {
    open fun foo(a: Int, b: String) {
    }
}

open class Middle : Super(), Trait {
    override fun foo(/*rename*/a: Int, b: String) {
    }
}

class Sub : Middle() {
    override fun foo(a: Int, b: String) {
    }
}
