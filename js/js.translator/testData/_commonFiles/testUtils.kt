package testUtils

fun isLegacyBackend(): Boolean =
    // Using eval to prevent DCE from thinking that following code depends on Kotlin module.
    eval("Boolean(Object.__legacyBackend__)").unsafeCast<Boolean>()

