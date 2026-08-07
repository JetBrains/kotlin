// WITH_STDLIB

// A value class with no exposure has no public JVM constructor at all - only 'constructor-impl' and
// 'box-impl'. Its stub must therefore declare no constructor at all - in particular not a phantom
// no-argument one that does not exist in the bytecode. 'Exposed' is the control: it does have a public one.

@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class Unexposed(val value: String)

@JvmInline
@JvmExposeBoxed
value class Exposed(val value: String)

// Exposing an operation does not expose construction: only 'consume' gets a boxed variant here.
@JvmExposeBoxed
fun consume(unexposed: Unexposed): String = unexposed.value
