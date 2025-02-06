// KIND: STANDALONE
// MODULE: InnerClass
// FILE: main.kt
class Outer {

    val outerProperty = 42

    inner class Inner {
        fun getOuterProperty(): Int {
            return outerProperty
        }

        inner class InnerInner {
            fun getOutPropertyFromInnerClass(): Int {
                return getOuterProperty()
            }
        }
    }
}