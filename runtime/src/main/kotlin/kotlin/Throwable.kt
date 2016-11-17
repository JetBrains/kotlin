package kotlin

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
public open class Throwable(open val message: String?, open val cause: Throwable?) {
    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    override fun toString(): String {
        /* enable, once codegen is improved.
        val s = "Throwable"
        return if (message != null) s + ": " + message.toString() else s */
        return "Throwable"
    }
}
