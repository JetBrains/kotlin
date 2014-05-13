package demo

class Container() {
    var myString: String = "1"
}

class One() {
    class object {
        var myContainer: Container = Container()
    }
}

class StringContainer(s: String) {}

class Test() {
    fun putString(s: String) {
    }
    fun test() {
        putString(One.myContainer.myString)
        StringContainer(One.myContainer.myString)
    }
}