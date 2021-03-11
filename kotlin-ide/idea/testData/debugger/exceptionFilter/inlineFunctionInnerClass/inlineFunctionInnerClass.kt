package inlineFunctionInnerClass

fun box() {
    A().Inner().test()
}

class A {
    inner class Inner {
        fun test() {
            foo {}
        }
    }
}

// MAIN_CLASS: inlineFunctionInnerClass.InlineFunctionInnerClassKt
// FILE: inlineFunctionFile.kt
// LINE: 4