open class Base<T>(name: T?)

open class One<T, K>(name: T?, private var mySecond: K?) : Base<T?>(name)