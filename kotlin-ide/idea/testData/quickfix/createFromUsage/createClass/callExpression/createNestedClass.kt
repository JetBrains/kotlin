// "Create class 'Nested'" "true"
class A {
    // TARGET_PARENT:
    class B {
        val a = <caret>Nested()
    }
}