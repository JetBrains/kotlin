class Base<T>(name: T) {
}

class One<T, K>(name: T, second: K) : Base<T>(name) {
    private var mySecond: K = 0

    {
        mySecond = second
    }
}