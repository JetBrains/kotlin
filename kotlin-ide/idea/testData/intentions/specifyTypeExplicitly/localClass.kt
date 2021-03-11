// IGNORE_FIR
class A {
    private fun bar() <caret>= {
        class Local()
        Local()
    }
}