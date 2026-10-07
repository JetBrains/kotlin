class WithBrokenInitializer {
    val broken: Int = unresolvedInitializer
}

class WithBrokenInitBlock {
    init {
        unresolvedInInit()
    }
}

class WithBrokenSecondaryConstructor(val x: Int) {
    constructor() : this(unresolvedArgument)

    fun healthy(): Int = x
}

fun withBrokenDefault(x: Int = unresolvedDefault): Int = x

class WithBrokenAccessor {
    val healthy: Int get() = 1
    val broken: Int get() = unresolvedInGetter
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
    check("unresolvedInitializer") { WithBrokenInitializer() }?.let { return it }
    check("unresolvedInInit") { WithBrokenInitBlock() }?.let { return it }

    if (WithBrokenSecondaryConstructor(42).healthy() != 42) return "Fail: primary constructor"
    check("unresolvedArgument") { WithBrokenSecondaryConstructor() }?.let { return it }

    if (withBrokenDefault(42) != 42) return "Fail: explicit argument"
    check("unresolvedDefault") { withBrokenDefault() }?.let { return it }

    val accessors = WithBrokenAccessor()
    if (accessors.healthy != 1) return "Fail: healthy accessor"
    check("unresolvedInGetter") { accessors.broken }?.let { return it }

    return "OK"
}
