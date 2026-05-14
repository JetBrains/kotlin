package test

class Outer {
    interface Nested {
        fun foo(): Int = 1
    }

    class NestedImpl : Nested
}
