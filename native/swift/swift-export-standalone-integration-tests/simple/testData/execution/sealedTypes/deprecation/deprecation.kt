// KIND: STANDALONE
// MODULE: SealedDeprecation
// FILE: deprecation.kt

// A warning-level inheritor keeps its case; an error-level one is unavailable in Swift and falls
// into `unknown`; a deprecated root still reports its inheritors.

package org.kotlin.foo

sealed class SealedClassNonDeprecated

@Deprecated("unavailable", level = DeprecationLevel.ERROR)
class DeprecatedErrorSubClass : SealedClassNonDeprecated() {
    override fun toString(): String = "DeprecatedErrorSubClass"
}

@Deprecated("deprecated")
class DeprecatedWarningSubClass : SealedClassNonDeprecated() {
    override fun toString(): String = "DeprecatedWarningSubClass"
}

// An error-level deprecated root: unavailable in Swift, so nothing here is referenceable from the
// test; the enum ends up with nothing but an `unknown` case.
@Deprecated("unavailable", level = DeprecationLevel.ERROR)
sealed class SealedClassDeprecatedError

@Suppress("DEPRECATION_ERROR")
class NonDeprecatedSubClassA : SealedClassDeprecatedError()

@Deprecated("deprecated")
sealed class SealedClassDeprecatedWarning

class NonDeprecatedSubClassB : SealedClassDeprecatedWarning() {
    override fun toString(): String = "NonDeprecatedSubClassB"
}

// FILE: factories.kt
package org.kotlin.foo

@Suppress("DEPRECATION")
fun createDeprecatedWarningSubClass(): SealedClassNonDeprecated = DeprecatedWarningSubClass()

@Suppress("DEPRECATION_ERROR")
fun createDeprecatedErrorSubClass(): SealedClassNonDeprecated = DeprecatedErrorSubClass()

@Suppress("DEPRECATION")
fun createNonDeprecatedSubClassB(): SealedClassDeprecatedWarning = NonDeprecatedSubClassB()
