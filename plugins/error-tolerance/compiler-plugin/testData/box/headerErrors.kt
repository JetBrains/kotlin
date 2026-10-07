fun withUnresolvedParameterType(x: UnresolvedType): Int = 1

fun callsFunctionWithErroneousHeader(): Int = withUnresolvedParameterType(TODO())

fun withUnresolvedReturnType(): UnresolvedReturnType = TODO()

class Container {
    fun healthy(): Int = 1
    fun withUnresolvedReturnType(): UnresolvedMemberType = TODO()
}

class WithUnresolvedSupertype : UnresolvedBase() {
    fun member(): Int = 1
}

fun withImplicitErroneousReturnType() = unresolvedCall()

fun callsFunctionWithImplicitErroneousReturnType(): Int {
    withImplicitErroneousReturnType()
    return 1
}

fun check(expectedMessagePart: String, block: () -> Unit): String? {
    try {
        block()
    } catch (e: Error) {
        val message = e.message.orEmpty()
        return if (message.contains(expectedMessagePart)) null else "Unexpected message: $message"
    }
    return "No error thrown, expected '$expectedMessagePart'"
}

fun box(): String {
    check("UnresolvedType") { callsFunctionWithErroneousHeader() }?.let { return it }
    if (Container().healthy() != 1) return "Fail: healthy member"
    check("UnresolvedBase") { WithUnresolvedSupertype() }?.let { return it }
    check("unresolvedCall") { callsFunctionWithImplicitErroneousReturnType() }?.let { return it }
    return "OK"
}
