package testUtils

@JsName("eval")
private external fun evalToBoolean(code: String): Boolean

fun isLegacyBackend(): Boolean =
    // Using eval to prevent DCE from thinking that following code depends on Kotlin module.
    evalToBoolean("(typeof Kotlin != \"undefined\" && typeof Kotlin.kotlin != \"undefined\")")