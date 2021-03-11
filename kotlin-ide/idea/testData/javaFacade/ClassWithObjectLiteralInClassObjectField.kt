package test

interface A

class <caret>B {
    companion object {
        fun bar() = object : A { }
    }
}
