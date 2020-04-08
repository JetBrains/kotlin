// "Create extension property 'A.foo'" "true"
class A(val n: Int)

class B {
    val A.test: Boolean get() = <caret>foo
}