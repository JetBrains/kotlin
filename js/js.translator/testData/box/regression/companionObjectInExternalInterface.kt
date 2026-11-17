// After inlining the extension properties, the unused companion object reference is eliminated
// (see RedundantVariableDeclarationElimination), so the nonexistent `I` must not be accessed at runtime.

public external interface I {
    companion object
}
public inline val I.Companion.O: I get() = "O".asDynamic().unsafeCast<I>()
public inline val I.Companion.K: I get() = "K".asDynamic().unsafeCast<I>()

fun box(): String {
    if (I.O != I.O) return "Fail 1"
    if (I.O == I.K) return "Fail 2"
    return I.O.unsafeCast<String>() + I.K.unsafeCast<String>()
}
