internal open class Base<T> internal constructor(name: T)

internal class One<T, K> internal constructor(name: T, private val mySecond: K) : Base<T>(name)