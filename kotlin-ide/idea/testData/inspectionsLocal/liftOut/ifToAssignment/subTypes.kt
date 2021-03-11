open class A
class B : A()
class C : A()

fun liftClass(boolean: Boolean) {
    val a1: A
    <caret>if (boolean) {
        a1 = B()
    } else {
        a1 = C()
    }
}