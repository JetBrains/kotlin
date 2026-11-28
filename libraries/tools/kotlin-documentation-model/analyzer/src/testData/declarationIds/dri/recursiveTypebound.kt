// MODULE: main
// FILE: example/RecursiveTypebound.kt
package example

// From DRITest.deep recursive typebound #1342.
 fun <T, S, R> recursiveBound(t: T, s: S, r: R) where T: List<S>, S: List<R>, R: List<S> = Unit
