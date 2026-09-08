// KIND: STANDALONE
// MODULE: SealedOptIn
// FILE: optin.kt

// `@RequiresOptIn` becomes a Swift SPI group named after the annotation's qualified Kotlin name,
// propagated onto the opt-in inheritor's case, its `_SealedType` struct and the nested enum. The
// non-opt-in siblings must stay reachable without any SPI import.

package org.kotlin.foo

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class OptInA

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class OptInB

sealed class SealedNonOptInClass

@OptInA
sealed class SealedOptInClass : SealedNonOptInClass()

class NonSealedNonOptInClassA : SealedNonOptInClass() {
    override fun toString(): String = "NonSealedNonOptInClassA"
}

/** Requires two opt-ins: its own and the branch's. */
@OptInB
@OptIn(OptInA::class)
class NonSealedOptInClass : SealedOptInClass() {
    override fun toString(): String = "NonSealedOptInClass"
}

@OptIn(OptInA::class)
class NonSealedNonOptInClassB : SealedOptInClass() {
    override fun toString(): String = "NonSealedNonOptInClassB"
}

// FILE: factories.kt
package org.kotlin.foo

fun createNonSealedNonOptInClassA(): SealedNonOptInClass = NonSealedNonOptInClassA()

@OptIn(OptInA::class, OptInB::class)
fun createNonSealedOptInClass(): SealedNonOptInClass = NonSealedOptInClass()

@OptIn(OptInA::class)
fun createNonSealedNonOptInClassB(): SealedNonOptInClass = NonSealedNonOptInClassB()
