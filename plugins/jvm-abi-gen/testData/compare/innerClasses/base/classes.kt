package test

class A {
    private inner class AInner {
        inner class AInnerInner {}

        fun aInnerMethod() {
            class AInnerMethodLocal

            val aInnerMethodLambda: (Int) -> Int = { it * it }
        }
    }

    private class ANested {
        inner class ANestedInner {}
        class ANestedNested
        object ANestedObject

        fun aNestedMethod() {
            class ANestedMethodLocal

            val aNestedMethodLambda: (Int) -> Int = { it * it }
        }
    }
}

private class B {
    inner class BInnerClass
    class BNestedClass
    object BObject {}
}