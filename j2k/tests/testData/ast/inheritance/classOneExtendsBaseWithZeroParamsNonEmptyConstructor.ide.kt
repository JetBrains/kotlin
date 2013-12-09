class Base(name: String) {
}

class One(name: String, second: String) : Base(name) {
    private var mySecond: String = 0

    {
        mySecond = second
    }
}