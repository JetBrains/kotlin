// MODULE: main
// FILE: toplevel/Issue634GenericNullableReceiver.kt
package toplevel

// From DRITest.issue634WithGenericNullableReceiver: self-bounded type parameter on a nullable receiver.
fun <T : Comparable<T>> T?.doSomethingWithNullable() = TODO()
