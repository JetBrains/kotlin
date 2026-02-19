// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-63507

annotation class AllOpen

@AllOpen
annotation class SubComponent(
    val scope: String
)
