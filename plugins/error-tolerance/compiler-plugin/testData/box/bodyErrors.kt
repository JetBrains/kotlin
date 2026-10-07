fun healthy(): Int = 42

fun unresolvedCall(): Int {
    return unresolved()
}

fun typeMismatch(): Int = "not an int"

fun errorInLambda(): Int {
    val list = listOf(1, 2, 3)
    return list.map { it.unresolvedMember }.size
}

inline fun expectError(expectedMessagePart: String, block: () -> Unit): String? {
    try {
        block()
    } catch (e: Error) {
        val message = e.message.orEmpty()
        return if (message.contains(expectedMessagePart)) null else "Unexpected message: $message"
    }
    return "No error thrown, expected '$expectedMessagePart'"
}

fun box(): String {
    if (healthy() != 42) return "Fail: healthy"
    expectError("Unresolved reference 'unresolved'") { unresolvedCall() }?.let { return it }
    expectError("Return type mismatch") { typeMismatch() }?.let { return it }
    expectError("Unresolved reference 'unresolvedMember'") { errorInLambda() }?.let { return it }
    return "OK"
}
