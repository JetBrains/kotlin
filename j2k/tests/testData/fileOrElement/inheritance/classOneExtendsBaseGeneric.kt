class Base<T>(name: T)

class One<T, K>(name: T, private val mySecond: K) : Base<T>(name)