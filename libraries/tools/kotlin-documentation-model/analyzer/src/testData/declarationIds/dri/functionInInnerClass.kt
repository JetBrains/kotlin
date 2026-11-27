// MODULE: main
// FILE: example/FunctionInInnerClass.kt
package example

// From DRITest.driForFunctionNestedInsideInnerClass: the inner function's return type refers
// to the outer class's type parameter.
class Sample<S>(first: S){
    inner class SampleInner {
       fun foo(): S = TODO()
    }
}
