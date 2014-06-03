class Base<T>(name: T)

class One<T, K>(name: T, private var mySecond: K) : Base<T>(name)