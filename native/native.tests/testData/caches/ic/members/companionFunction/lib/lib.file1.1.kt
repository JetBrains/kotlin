package test

class A {
    companion object {
        fun foo() = 2
        inline fun inlineFoo() = 200
    }
}

class Outer {
    class B {
        companion object {
            fun bar() = 20
        }
    }
}
