internal open class Base<T>(name: T?)

internal class One<T, K>(name: T?, private val mySecond: K?) : Base<T?>(name)