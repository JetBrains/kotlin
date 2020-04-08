// "Replace with 'Exception()'" "true"
// RUNTIME_WITH_FULL_JDK
package ppp

@Deprecated("do not use", ReplaceWith("Exception()"))
fun x(): Throwable = RuntimeException()

val e = <caret>x()