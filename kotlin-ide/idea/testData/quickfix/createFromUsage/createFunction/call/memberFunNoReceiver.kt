// "Create function 'foo'" "true"

class A {
    class B {
        fun test(): Int {
            return <caret>foo(2, "2")
        }
    }
}