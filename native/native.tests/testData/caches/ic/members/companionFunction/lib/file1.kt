package test

class A {
    companion object {
        fun foo() = 1
        inline fun inlineFoo() = 100
    }
}

class Outer {
    class B {
        companion object {
            fun bar() = 10
        }
    }
}
