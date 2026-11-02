package lib

object Holder {
    // The property is private, so its type is the anonymous class itself rather than kotlin.Any.
    private val anonymous = object {
        fun value() = "anonymous"
    }

    val value: String
        get() = anonymous.value()
}
