package testUtils

fun isLegacyBackend(): Boolean =
    // Using eval to prevent DCE from thinking that following code depends on Kotlin module.
    eval("(typeof Kotlin != \"undefined\" && typeof Kotlin.kotlin != \"undefined\")").unsafeCast<Boolean>()

