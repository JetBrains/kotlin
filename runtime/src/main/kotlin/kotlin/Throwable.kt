package kotlin

/**
 * The base class for all errors and exceptions. Only instances of this class can be thrown or caught.
 *
 * @param message the detail message string.
 * @param cause the cause of this throwable.
 */
@ExportTypeInfo("theThrowableTypeInfo")
public open class Throwable(open val message: String?, open val cause: Throwable?) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    private val stacktrace: Array<String> = getCurrentStackTrace()

    fun printStackTrace() {
        println(this.toString())
        for (element in stacktrace) {
            println("        at " + element)
        }

        this.cause?.printEnclosedStackTrace(this)
    }

    private fun printEnclosedStackTrace(enclosing: Throwable) {
        // TODO: should skip common stack frames
        print("Caused by: ")
        this.printStackTrace()
    }

    override fun toString(): String {
        val s = "Throwable" // TODO: should be class name
        return if (message != null) s + ": " + message.toString() else s
    }
}

@SymbolName("Kotlin_getCurrentStackTrace")
private external fun getCurrentStackTrace(): Array<String>