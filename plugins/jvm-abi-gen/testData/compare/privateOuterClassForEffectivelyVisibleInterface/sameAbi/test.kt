package test

class Outer {
    class Public : NestedClass.I

    private class NestedClass {
        interface I

        fun someFun() = Unit
    }
}
