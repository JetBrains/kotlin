// MODULE: main
// FILE: example/GenericMemberFunction.kt
package example

// From DRITest.driForGenericFunction. `Tuple` is intentionally unresolved, as in the original.
class Sample<S>(first: S){
    fun <T> genericFun(param1: String): Tuple<S,T> = TODO()
}
