// KIND: STANDALONE
// MODULE: InnerClass
// FILE: main.kt
class Outer {
    inner class Inner {
        inner class InnerInner
    }
}

class OuterWithParam(val outerParam: Int) {
    inner class InnerWithParam(val innerParamA: Int, val innerParamB: Int) {
        inner class InnerInnerWithParam(val innerInnerParam: Int) {
            fun getOuter(): Int = outerParam
            fun getInnerA(): Int = innerParamA
            fun getInnerB(): Int = innerParamB
            fun getInnerInner(): Int = innerInnerParam
        }
    }
}