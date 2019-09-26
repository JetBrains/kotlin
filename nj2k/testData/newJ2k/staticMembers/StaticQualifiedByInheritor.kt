open class Base {
    companion object {
        fun foo() {}
    }
}

class Derived : Base()
object User {
    fun test() {
        Base.foo()
    }
}