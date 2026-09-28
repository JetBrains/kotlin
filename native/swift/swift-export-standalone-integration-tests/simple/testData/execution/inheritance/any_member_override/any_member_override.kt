// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: any_member_override.kt

// A Kotlin class that overrides the members of kotlin.Any. Its implementations have to be visible
// through the NSObject spellings (-description/-hash/-isEqual:), and a Swift subclass overriding
// those spellings has to win over them -- including when it delegates to `super`.
//
// `toString`/`hashCode`/`equals` are deliberately not exported under their Kotlin names. Exporting
// them installed a second reverse bridge into the very same vtable slot, and the Kotlin-named one
// won it: Kotlin dispatch went straight to the Kotlin implementation, never sending the selector,
// so the native override was invisible from Kotlin. See KT-88259.

open class Describable {
    override fun toString(): String = "kotlin-describable"
    override fun hashCode(): Int = 11
    override fun equals(other: Any?): Boolean = other is Describable
}

// Overridden twice in Kotlin: resolving a member of a Swift subclass has to reach the *nearest*
// compiled implementation, not the least derived one.
open class DescribableBase {
    override fun toString(): String = "kotlin-base"
    override fun hashCode(): Int = 1
}

open class DescribableDerived : DescribableBase() {
    override fun toString(): String = "kotlin-derived"
    override fun hashCode(): Int = 2
}

// Only `toString` is overridden; `hashCode` and `equals` stay with kotlin.Any. Each slot is resolved
// on its own, so a Swift subclass can own one while Kotlin owns another.
open class PartiallyDescribable {
    override fun toString(): String = "kotlin-partial"
}

fun callToString(value: Any): String = value.toString()
fun callHashCode(value: Any): Int = value.hashCode()
fun callEquals(lhs: Any, rhs: Any): Boolean = lhs == rhs

// `hashCode`/`equals` have to agree with the Swift side well enough for the two runtimes to place
// the same object in the same bucket.
fun hashSetContains(element: Any, probe: Any): Boolean = hashSetOf(element).contains(probe)
